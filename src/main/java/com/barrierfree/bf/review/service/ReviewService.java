package com.barrierfree.bf.review.service;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.global.service.JinaEmbeddingService;
import com.barrierfree.bf.global.service.ImageService;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.review.dto.FacilityCountDto;
import com.barrierfree.bf.review.dto.ReviewCreateRequest;
import com.barrierfree.bf.review.dto.ReviewResponse;
import com.barrierfree.bf.review.entity.Review;
import com.barrierfree.bf.review.repository.ReviewRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final JinaEmbeddingService jinaEmbeddingService; // GeminiService에서 Jina API로 마이그레이션
    private final ImageService imageService;

    /**
     * [리뷰 작성]
     * 1. 사진을 R2에 업로드
     * 2. 리뷰 텍스트를 조합하여 Jina Embeddings v5에서 임베딩 벡터 추출 (task: retrieval.passage)
     * 3. DB에 저장
     */
    @Transactional
    public void createReview(Long userId, String placeId, ReviewCreateRequest request, List<MultipartFile> images) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. R2 버킷에 이미지 업로드 및 URL 반환
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String uploadedUrl = imageService.uploadImage("reviews", image);
                if (uploadedUrl != null) {
                    imageUrls.add(uploadedUrl);
                }
            }
        }

        // 2. 의미 기반 검색을 위한 텍스트 조합 및 임베딩 추출 (Jina API)
        String facilitiesText = request.getFacilities() != null && !request.getFacilities().isEmpty()
                ? String.join(", ", request.getFacilities())
                : "없음";
        
        String textForEmbedding = String.format("장소명: %s, 카테고리: %s, 시설: %s, 리뷰내용: %s",
                request.getPlaceName(), request.getCategory(), facilitiesText, request.getContent());
        
        // 데이터베이스에 "저장"하는 목적이므로 retrieval.passage 태스크 사용
        float[] embedding = jinaEmbeddingService.getEmbedding(textForEmbedding, "retrieval.passage");

        // 3. String 리스트를 실제 Enum 리스트로 안전하게 변환
        List<MobilityType> mobilities = parseMobilities(request.getMobilities());
        List<FacilityType> facilities = parseFacilities(request.getFacilities());
        PlaceCategory category = parseCategory(request.getCategory());

        // 4. 엔티티 생성 및 저장 (embedding은 이제 1024차원 배열)
        Review review = Review.builder()
                .user(user)
                .placeId(placeId)
                .placeName(request.getPlaceName())
                .category(category)
                .rating(request.getRating())
                .content(request.getContent())
                .embedding(embedding)
                .mobilities(mobilities)
                .facilities(facilities)
                .imageUrls(imageUrls)
                .build();

        reviewRepository.save(review);
    }

    /**
     * [전체 리뷰 조회 (글로벌 필터링)]
     */
    public Page<ReviewResponse> getAllReviews(String categoryStr, Integer minRating, List<String> mobilities, List<String> facilities, Pageable pageable) {
        return getPlaceReviews(null, categoryStr, minRating, mobilities, facilities, pageable);
    }

    /**
     * [특정 장소의 리뷰 조회]
     * Repository의 동적 쿼리에 맞춰 안전하게 데이터를 정제하여 전달합니다.
     */
    public Page<ReviewResponse> getPlaceReviews(String placeId, String categoryStr, Integer minRating, List<String> mobilities, List<String> facilities, Pageable pageable) {
        PlaceCategory category = categoryStr != null ? parseCategory(categoryStr) : null;
        
        List<MobilityType> mobilityTypes = parseMobilities(mobilities);
        boolean hasMobilities = !mobilityTypes.isEmpty();

        List<FacilityType> facilityTypes = parseFacilities(facilities);
        boolean hasFacilities = !facilityTypes.isEmpty();

        Page<Review> reviews = reviewRepository.findFilteredReviews(
                placeId, category, minRating, 
                mobilityTypes, hasMobilities, 
                facilityTypes, hasFacilities, 
                pageable
        );

        // @BatchSize가 걸려있으므로 DTO 변환 시 지연 로딩(Lazy Loading)으로 인한 N+1이 발생하지 않습니다.
        return reviews.map(ReviewResponse::from);
    }

    /**
     * [시설 통계 카운트]
     * 특정 장소에 등록된 시설별 리뷰 수를 DB 그룹 연산으로 빠르게 가져옵니다.
     */
    public List<FacilityCountDto> getFacilityCounts(String placeId) {
        return reviewRepository.countFacilitiesByPlaceId(placeId);
    }

    /**
     * [자연어 기반 임베딩 검색]
     * 프론트엔드의 검색어를 Jina API(retrieval.query)로 벡터 변환한 뒤, pgvector 유사도 쿼리로 리뷰를 찾아옵니다.
     */
    public Page<ReviewResponse> searchSimilarReviews(String query, Pageable pageable) {
        // 1. 검색어를 벡터로 변환 (사용자의 "질의"이므로 retrieval.query 태스크 사용)
        float[] queryEmbedding = jinaEmbeddingService.getEmbedding(query, "retrieval.query");
        
        // 2. float 배열을 PostgreSQL vector 타입 문법 문자열로 변환 (예: "[0.1, 0.2, ...]")
        String embeddingString = Arrays.toString(queryEmbedding);

        // 3. 네이티브 쿼리 실행 (DB의 embedding 컬럼은 vector(1024) 타입이어야 함)
        Page<Review> reviews = reviewRepository.findSimilarReviewsByEmbedding(embeddingString, pageable);
        
        return reviews.map(ReviewResponse::from);
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

    private List<MobilityType> parseMobilities(List<String> mobilities) {
        if (mobilities == null || mobilities.isEmpty()) return new ArrayList<>();
        try {
            return mobilities.stream().map(MobilityType::valueOf).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<FacilityType> parseFacilities(List<String> facilities) {
        if (facilities == null || facilities.isEmpty()) return new ArrayList<>();
        try {
            return facilities.stream().map(FacilityType::valueOf).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}