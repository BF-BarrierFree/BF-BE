package com.barrierfree.bf.review.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.review.entity.Review;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 프론트엔드로 반환할 리뷰 조회 응답 DTO */
@Getter
@Builder
public class ReviewResponse {

  private Long reviewId;
  private String nickname;
  private String profileImageUrl;
  private String placeId;
  private String placeName;
  private Integer rating;
  private String content;
  private List<MobilityType> mobilities;
  private List<FacilityType> facilities;
  private List<String> imageUrls;
  private LocalDateTime createdAt;

  /** Entity를 DTO로 변환하는 정적 팩토리 메서드 (컨벤션 준수) */
  public static ReviewResponse from(Review review) {
    return ReviewResponse.builder()
        .reviewId(review.getId())
        .nickname(review.getUser().getNickname())
        .profileImageUrl(review.getUser().getProfileImageUrl())
        .placeId(review.getPlaceId())
        .placeName(review.getPlaceName())
        .rating(review.getRating())
        .content(review.getContent())
        .mobilities(review.getMobilities())
        .facilities(review.getFacilities())
        .imageUrls(review.getImageUrls())
        .createdAt(review.getCreatedAt())
        .build();
  }
}
