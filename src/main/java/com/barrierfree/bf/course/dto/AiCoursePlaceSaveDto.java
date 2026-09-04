package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;

/**
 * AI 코스에 포함된 개별 장소 데이터를 DB에 저장하기 위한 DTO
 */
public record AiCoursePlaceSaveDto(
    String placeId,
    String name,
    PlaceCategory category,
    String address,
    Double latitude,
    Double longitude,
    String photoUrl
) {}
