package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import jakarta.validation.constraints.NotNull;

/**
 * AI 코스에 포함된 개별 장소 데이터를 DB에 저장하기 위한 DTO
 */
public record AiCoursePlaceSaveDto(
    @NotNull(message = "장소 ID는 필수입니다.") String placeId,
    @NotNull(message = "장소 이름은 필수입니다.") String name,
    PlaceCategory category,
    String address,
    @NotNull(message = "위도는 필수입니다.") Double latitude,
    @NotNull(message = "경도는 필수입니다.") Double longitude,
    String photoUrl
) {}
