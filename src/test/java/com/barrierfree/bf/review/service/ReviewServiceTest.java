package com.barrierfree.bf.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.global.service.ImageService;
import com.barrierfree.bf.global.service.JinaEmbeddingService;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.review.dto.*;
import com.barrierfree.bf.review.entity.*;
import com.barrierfree.bf.review.repository.*;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.*;

class ReviewServiceTest {
  private final ReviewRepository reviews = mock(ReviewRepository.class);
  private final ReviewHelpfulRepository helpfuls = mock(ReviewHelpfulRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final JinaEmbeddingService embeddings = mock(JinaEmbeddingService.class);
  private final ImageService images = mock(ImageService.class);
  private final ReviewService service =
      new ReviewService(reviews, helpfuls, users, embeddings, images);
  private final Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
  private Review review;

  @BeforeEach
  void setup() {
    User user = User.builder().nickname("작성자").build();
    ReflectionTestUtils.setField(user, "id", 1L);
    review =
        Review.builder()
            .user(user)
            .placeId("place")
            .placeName("장소")
            .category(PlaceCategory.FOOD)
            .region("서울 용산구")
            .content("이전 내용")
            .imageUrls(List.of("old-a", "old-b"))
            .build();
    ReflectionTestUtils.setField(review, "id", 10L);
    when(users.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(reviews.findById(10L)).thenReturn(Optional.of(review));
    when(embeddings.getEmbedding(anyString(), eq("retrieval.passage"))).thenReturn(new float[] {1});
  }

  @Test
  void createsWithoutRatingAndNormalizesRegion() {
    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setPlaceName("장소");
    request.setCategory("FOOD");
    request.setContent("내용");
    request.setRegion(" 서울   용산구 ");
    service.createReview(1L, "place", request, null);
    verify(reviews)
        .save(argThat(r -> r.getRegion().equals("서울 용산구") && r.getPlaceId().equals("place")));
  }

  @Test
  void globalFilterPassesRegionAndIncludesHelpfulCount() {
    when(reviews.findFilteredReviews(
            isNull(),
            eq(PlaceCategory.FOOD),
            eq("서울"),
            anyList(),
            eq(false),
            anyList(),
            eq(false),
            eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(review), pageable, 1));
    ReviewHelpfulRepository.HelpfulCount count = mock(ReviewHelpfulRepository.HelpfulCount.class);
    when(count.getReviewId()).thenReturn(10L);
    when(count.getCount()).thenReturn(24L);
    when(helpfuls.countByReviewIds(List.of(10L))).thenReturn(List.of(count));
    ReviewResponse response =
        service.getAllReviews("FOOD", " 서울 ", null, null, pageable).getContent().getFirst();
    assertThat(response.getHelpfulCount()).isEqualTo(24);
    assertThat(response.getCategory()).isEqualTo(PlaceCategory.FOOD);
    assertThat(response.getRegion()).isEqualTo("서울 용산구");
    verify(helpfuls, times(1)).countByReviewIds(List.of(10L));
  }

  @Test
  void myAndHelpfulAndPlaceAndSearchListsDefaultCountToZero() {
    Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);
    when(reviews.findAllByUserIdAndIsDeletedFalse(1L, pageable)).thenReturn(page);
    when(helpfuls.findActiveHelpfulReviewsByUserId(1L, pageable))
        .thenReturn(
            new PageImpl<>(List.of(new ReviewHelpful(review.getUser(), review)), pageable, 1));
    when(reviews.findFilteredReviews(
            eq("place"),
            isNull(),
            isNull(),
            anyList(),
            eq(false),
            anyList(),
            eq(false),
            eq(pageable)))
        .thenReturn(page);
    when(embeddings.getEmbedding("검색", "retrieval.query")).thenReturn(new float[] {1});
    when(reviews.findSimilarReviewsByEmbedding("[1.0]", pageable)).thenReturn(page);
    for (Page<ReviewResponse> result :
        List.of(
            service.getMyReviews(1L, pageable),
            service.getMyHelpfulReviews(1L, pageable),
            service.getPlaceReviews("place", null, null, null, null, pageable),
            service.searchSimilarReviews("검색", pageable))) {
      assertThat(result.getContent().getFirst().getHelpfulCount()).isZero();
      assertThat(result.getContent().getFirst().getCategory()).isEqualTo(PlaceCategory.FOOD);
    }
  }

  @Test
  void rejectsRemovedRatingSortAndWildcardRegion() {
    assertThatThrownBy(
            () ->
                service.getAllReviews(
                    null, null, null, null, PageRequest.of(0, 10, Sort.by("rating"))))
        .isInstanceOf(CustomException.class);
    assertThatThrownBy(() -> service.getAllReviews(null, "%", null, null, pageable))
        .isInstanceOf(CustomException.class);
    verifyNoInteractions(reviews);
  }

  @Test
  void updatePreservesImagesWhenOmittedAndRefreshesEmbedding() {
    service.updateReview(
        1L,
        10L,
        new ReviewUpdateRequest("수정", List.of("WHEELCHAIR"), List.of("RAMP"), null, null),
        null);
    assertThat(review.getImageUrls()).containsExactly("old-a", "old-b");
    assertThat(review.getContent()).isEqualTo("수정");
    assertThat(review.getEmbedding()).containsExactly(1);
    verify(embeddings).getEmbedding(contains("리뷰내용: 수정"), eq("retrieval.passage"));
  }

  @Test
  void updateRetainsSelectedImagesAddsFilesAndDeletesOldOnlyAfterCommit() {
    MockMultipartFile file =
        new MockMultipartFile("images", "new.png", "image/png", new byte[] {1});
    when(images.uploadImage("reviews", file))
        .thenReturn(new ImageService.UploadedImage("new-key", "new-url"));
    when(images.extractObjectKey("old-a")).thenReturn("old-key");
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.updateReview(
          1L,
          10L,
          new ReviewUpdateRequest("수정", null, null, List.of("old-b"), "부산 해운대구"),
          List.of(file));
      assertThat(review.getImageUrls()).containsExactly("old-b", "new-url");
      assertThat(review.getRegion()).isEqualTo("부산 해운대구");
      verify(images, never()).deleteImage(any());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(images).deleteImage("old-key");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void emptyRetainedListClearsAllImages() {
    service.updateReview(1L, 10L, new ReviewUpdateRequest("수정", null, null, List.of(), null), null);
    assertThat(review.getImageUrls()).isEmpty();
  }

  @Test
  void rejectsForeignImageBeforeUploading() {
    assertThatThrownBy(
            () ->
                service.updateReview(
                    1L,
                    10L,
                    new ReviewUpdateRequest("수정", null, null, List.of("someone-elses-url"), null),
                    null))
        .isInstanceOf(CustomException.class);
    verifyNoInteractions(images, embeddings);
  }

  @Test
  void rollbackCleansNewUploadAndPreservesOldObject() {
    MockMultipartFile file =
        new MockMultipartFile("images", "new.png", "image/png", new byte[] {1});
    when(images.uploadImage("reviews", file))
        .thenReturn(new ImageService.UploadedImage("new-key", "new-url"));
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.updateReview(
          1L, 10L, new ReviewUpdateRequest("수정", null, null, List.of(), null), List.of(file));
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(images).deleteImage("new-key");
      verify(images, never()).extractObjectKey(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void ownerCanSoftDeleteAndDeletedReviewCannotBeUpdatedOrLiked() {
    service.deleteReview(1L, 10L);
    assertThat(review.isDeleted()).isTrue();
    assertThat(review.getDeletedAt()).isNotNull();
    assertThatThrownBy(
            () ->
                service.updateReview(
                    1L, 10L, new ReviewUpdateRequest("수정", null, null, null, null), null))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    assertThatThrownBy(() -> service.markReviewHelpful(1L, 10L))
        .isInstanceOf(CustomException.class);
  }

  @Test
  void nonOwnerCannotDeleteOrUpdate() {
    when(users.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(User.builder().build()));
    assertThatThrownBy(() -> service.deleteReview(2L, 10L))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REVIEW_UNAUTHORIZED_ACCESS));
    assertThatThrownBy(
            () ->
                service.updateReview(
                    2L, 10L, new ReviewUpdateRequest("수정", null, null, null, null), null))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REVIEW_UNAUTHORIZED_ACCESS));
    assertThat(review.isDeleted()).isFalse();
    verifyNoInteractions(images, embeddings);
  }
}
