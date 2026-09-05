package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import lombok.Builder;

/**
 * AI 코스 미리보기에 포함되는 개별 장소 정보 DTO
 */
@Builder
public record AiCoursePlacePreview(
    Integer sequence,         // 코스 내 순서 (0, 1, 2...)
    String placeId,           // 구글 PlaceId 또는 공공데이터 ContentId (PUBLIC_DATA:123)
    String name,              // 장소명
    PlaceCategory category,   // 카테고리
    String categoryLabel,     // 카테고리 한글명
    String address,           // 주소
    Double latitude,
    Double longitude,
    String photoUrl,          // 대표 사진 URL
    String accessibilityDataSource // 배리어프리 정보 출처
) {
    public static AiCoursePlacePreview from(Integer sequence, PlaceSearchResponse.PlaceSummary summary) {
        return AiCoursePlacePreview.builder()
            .sequence(sequence)
            .placeId(summary.placeId())
            .name(summary.name())
            .category(summary.category())
            .categoryLabel(summary.categoryLabel())
            .address(summary.address())
            .latitude(summary.lat())
            .longitude(summary.lng())
            .photoUrl(summary.photoUrl())
            .accessibilityDataSource(summary.accessibilityDataSource())
            .build();
    }
}
