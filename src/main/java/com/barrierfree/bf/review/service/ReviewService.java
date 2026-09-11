package com.barrierfree.bf.review.service;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.global.service.ImageService;
import com.barrierfree.bf.global.service.JinaEmbeddingService;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.review.dto.FacilityCountDto;
import com.barrierfree.bf.review.dto.ReviewCreateRequest;
import com.barrierfree.bf.review.dto.ReviewResponse;
import com.barrierfree.bf.review.dto.ReviewUpdateRequest;
import com.barrierfree.bf.review.entity.Review;
import com.barrierfree.bf.review.entity.ReviewHelpful;
import com.barrierfree.bf.review.repository.ReviewHelpfulRepository;
import com.barrierfree.bf.review.repository.ReviewRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewHelpfulRepository reviewHelpfulRepository;
  private final UserRepository userRepository;
  private final JinaEmbeddingService jinaEmbeddingService; // GeminiService에서 Jina API로 마이그레이션
  private final ImageService imageService;

  /**
   * [리뷰 작성] 1. 사진을 R2에 업로드 2. 리뷰 텍스트를 조합하여 Jina Embeddings v5에서 임베딩 벡터 추출 (task: retrieval.passage)
   * 3. DB에 저장
   */
  @Transactional
  public void createReview(
      Long userId, String placeId, ReviewCreateRequest request, List<MultipartFile> images) {
    User user = findUser(userId);
    List<MobilityType> mobilities = parseMobilities(request.getMobilities());
    List<FacilityType> facilities = parseFacilities(request.getFacilities());
    PlaceCategory category = parseCategory(request.getCategory());
    String region = normalizeRegion(request.getRegion());
    if (region == null || (request.getPlaceId() != null && !placeId.equals(request.getPlaceId()))) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    // 2. 의미 기반 검색을 위한 텍스트 조합 및 임베딩 추출 (Jina API)
    String facilitiesText =
        request.getFacilities() != null && !request.getFacilities().isEmpty()
            ? String.join(", ", request.getFacilities())
            : "없음";

    String textForEmbedding =
        String.format(
            "장소명: %s, 카테고리: %s, 시설: %s, 리뷰내용: %s",
            request.getPlaceName(), request.getCategory(), facilitiesText, request.getContent());

    // 데이터베이스에 "저장"하는 목적이므로 retrieval.passage 태스크 사용
    float[] embedding = jinaEmbeddingService.getEmbedding(textForEmbedding, "retrieval.passage");

    List<String> imageUrls = uploadImages(images);

    // 4. 엔티티 생성 및 저장 (embedding은 이제 1024차원 배열)
    Review review =
        Review.builder()
            .user(user)
            .placeId(placeId)
            .placeName(request.getPlaceName())
            .category(category)
            .region(region)
            .content(request.getContent())
            .embedding(embedding)
            .mobilities(mobilities)
            .facilities(facilities)
            .imageUrls(imageUrls)
            .build();

    reviewRepository.save(review);
  }

  /** [전체 리뷰 조회 (글로벌 필터링)] */
  public Page<ReviewResponse> getAllReviews(
      String categoryStr,
      String region,
      List<String> mobilities,
      List<String> facilities,
      Pageable pageable) {
    return getPlaceReviews(null, categoryStr, region, mobilities, facilities, pageable);
  }

  /** [특정 장소의 리뷰 조회] Repository의 동적 쿼리에 맞춰 안전하게 데이터를 정제하여 전달합니다. */
  public Page<ReviewResponse> getPlaceReviews(
      String placeId,
      String categoryStr,
      String region,
      List<String> mobilities,
      List<String> facilities,
      Pageable pageable) {
    validateSort(pageable);
    PlaceCategory category = categoryStr != null ? parseCategory(categoryStr) : null;

    List<MobilityType> mobilityTypes = parseMobilities(mobilities);
    boolean hasMobilities = !mobilityTypes.isEmpty();

    List<FacilityType> facilityTypes = parseFacilities(facilities);
    boolean hasFacilities = !facilityTypes.isEmpty();

    Page<Review> reviews =
        reviewRepository.findFilteredReviews(
            placeId,
            category,
            normalizeRegion(region),
            mobilityTypes,
            hasMobilities,
            facilityTypes,
            hasFacilities,
            pageable);

    return toResponses(reviews);
  }

  /** [시설 통계 카운트] 특정 장소에 등록된 시설별 리뷰 수를 DB 그룹 연산으로 빠르게 가져옵니다. */
  public List<FacilityCountDto> getFacilityCounts(String placeId) {
    return reviewRepository.countFacilitiesByPlaceId(placeId);
  }

  /**
   * [자연어 기반 임베딩 검색] 프론트엔드의 검색어를 Jina API(retrieval.query)로 벡터 변환한 뒤, pgvector 유사도 쿼리로 리뷰를 찾아옵니다.
   */
  public Page<ReviewResponse> searchSimilarReviews(String query, Pageable pageable) {
    validateSort(pageable);
    // 1. 검색어를 벡터로 변환 (사용자의 "질의"이므로 retrieval.query 태스크 사용)
    float[] queryEmbedding = jinaEmbeddingService.getEmbedding(query, "retrieval.query");

    // 2. float 배열을 PostgreSQL vector 타입 문법 문자열로 변환 (예: "[0.1, 0.2, ...]")
    String embeddingString = Arrays.toString(queryEmbedding);

    // 3. 네이티브 쿼리 실행 (DB의 embedding 컬럼은 vector(1024) 타입이어야 함)
    Page<Review> reviews =
        reviewRepository.findSimilarReviewsByEmbedding(embeddingString, pageable);

    return toResponses(reviews);
  }

  /** 현재 사용자가 작성한 삭제되지 않은 리뷰를 조회합니다. */
  public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
    findUser(userId);
    validateSort(pageable);
    return toResponses(reviewRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable));
  }

  /** 현재 사용자가 도움이 되었다고 표시한 활성 리뷰를 조회합니다. */
  public Page<ReviewResponse> getMyHelpfulReviews(Long userId, Pageable pageable) {
    findUser(userId);
    validateSort(pageable);
    return toResponses(
        reviewHelpfulRepository
            .findActiveHelpfulReviewsByUserId(userId, pageable)
            .map(ReviewHelpful::getReview));
  }

  @Transactional
  public void markReviewHelpful(Long userId, Long reviewId) {
    User user = findUser(userId);
    Review review = findActiveReview(reviewId);
    if (!reviewHelpfulRepository.existsByUserIdAndReviewId(userId, reviewId)) {
      reviewHelpfulRepository.save(new ReviewHelpful(user, review));
    }
  }

  @Transactional
  public void unmarkReviewHelpful(Long userId, Long reviewId) {
    findUser(userId);
    reviewHelpfulRepository
        .findByUserIdAndReviewId(userId, reviewId)
        .ifPresent(reviewHelpfulRepository::delete);
  }

  @Transactional
  public void updateReview(
      Long userId, Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images) {
    Review review = findOwnedReview(userId, reviewId);
    List<MobilityType> mobilities = parseMobilities(request.mobilities());
    List<FacilityType> facilities = parseFacilities(request.facilities());
    List<String> retained =
        request.retainedImageUrls() == null
            ? new ArrayList<>(review.getImageUrls())
            : new ArrayList<>(request.retainedImageUrls());
    if (!review.getImageUrls().containsAll(retained)
        || retained.stream().distinct().count() != retained.size()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    String region =
        request.region() == null ? review.getRegion() : normalizeRegion(request.region());
    if (request.region() != null && region == null) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    String facilitiesText =
        facilities.isEmpty()
            ? "없음"
            : facilities.stream().map(Enum::name).collect(Collectors.joining(", "));
    float[] embedding =
        jinaEmbeddingService.getEmbedding(
            String.format(
                "장소명: %s, 카테고리: %s, 시설: %s, 리뷰내용: %s",
                review.getPlaceName(), review.getCategory(), facilitiesText, request.content()),
            "retrieval.passage");
    List<String> removed =
        review.getImageUrls().stream().filter(url -> !retained.contains(url)).toList();
    retained.addAll(uploadImages(images));
    review.update(request.content(), mobilities, facilities, retained, region, embedding);
    afterCommitDelete(removed);
  }

  @Transactional
  public void deleteReview(Long userId, Long reviewId) {
    findOwnedReview(userId, reviewId).softDelete();
  }

  private Review findOwnedReview(Long userId, Long reviewId) {
    findUser(userId);
    Review review = findActiveReview(reviewId);
    if (!review.getUser().getId().equals(userId)) {
      throw new CustomException(ErrorCode.REVIEW_UNAUTHORIZED_ACCESS);
    }
    return review;
  }

  private Page<ReviewResponse> toResponses(Page<Review> reviews) {
    if (reviews.isEmpty()) return reviews.map(review -> ReviewResponse.from(review, 0));
    Map<Long, Long> counts =
        reviewHelpfulRepository
            .countByReviewIds(reviews.getContent().stream().map(Review::getId).toList())
            .stream()
            .collect(
                Collectors.toMap(
                    ReviewHelpfulRepository.HelpfulCount::getReviewId,
                    ReviewHelpfulRepository.HelpfulCount::getCount));
    return reviews.map(
        review -> ReviewResponse.from(review, counts.getOrDefault(review.getId(), 0L)));
  }

  private void validateSort(Pageable pageable) {
    if (pageable.getSort().stream()
        .anyMatch(order -> !List.of("createdAt", "id").contains(order.getProperty()))) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private String normalizeRegion(String region) {
    if (region == null || region.isBlank()) return null;
    String normalized = region.trim().replaceAll("\\s+", " ");
    if (normalized.length() > 100 || !normalized.matches("[가-힣a-zA-Z0-9 ·-]+")) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return normalized;
  }

  private List<String> uploadImages(List<MultipartFile> images) {
    List<ImageService.UploadedImage> uploaded = new ArrayList<>();
    boolean synchronizedTransaction = TransactionSynchronizationManager.isSynchronizationActive();
    if (synchronizedTransaction) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED)
                uploaded.forEach(image -> imageService.deleteImage(image.objectKey()));
            }
          });
    }
    try {
      if (images != null) {
        for (MultipartFile image : images) {
          ImageService.UploadedImage result = imageService.uploadImage("reviews", image);
          if (result != null) uploaded.add(result);
        }
      }
      return uploaded.stream().map(ImageService.UploadedImage::publicUrl).toList();
    } catch (RuntimeException e) {
      if (!synchronizedTransaction)
        uploaded.forEach(image -> imageService.deleteImage(image.objectKey()));
      throw e;
    }
  }

  private void afterCommitDelete(List<String> urls) {
    if (urls.isEmpty()) return;
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              urls.forEach(url -> imageService.deleteImage(imageService.extractObjectKey(url)));
            }
          });
    }
  }

  // =========================================================================
  // 안전한 타입 변환을 위한 Helper 메서드들
  // =========================================================================

  private PlaceCategory parseCategory(String category) {
    try {
      return PlaceCategory.valueOf(category);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private User findUser(Long userId) {
    if (userId == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }
    return userRepository
        .findByIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  private Review findActiveReview(Long reviewId) {
    return reviewRepository
        .findById(reviewId)
        .filter(review -> !review.isDeleted())
        .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
  }

  private List<MobilityType> parseMobilities(List<String> mobilities) {
    if (mobilities == null || mobilities.isEmpty()) return new ArrayList<>();
    try {
      return mobilities.stream().map(MobilityType::valueOf).collect(Collectors.toList());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private List<FacilityType> parseFacilities(List<String> facilities) {
    if (facilities == null || facilities.isEmpty()) return new ArrayList<>();
    try {
      return facilities.stream().map(FacilityType::valueOf).collect(Collectors.toList());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }
}
