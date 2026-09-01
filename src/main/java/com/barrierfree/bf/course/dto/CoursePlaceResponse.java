package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.course.entity.CoursePlace;
import com.barrierfree.bf.place.domain.PlaceCategory;
import lombok.Builder;

@Builder
public record CoursePlaceResponse(
    Long id,
    Integer sequence,
    String name,
    PlaceCategory category,
    String address,
    Double latitude,
    Double longitude,
    String photoUrl,
    String distanceToNext,
    String movingTip) {
  public static CoursePlaceResponse from(CoursePlace place) {
    return CoursePlaceResponse.builder()
        .id(place.getId())
        .sequence(place.getSequence())
        .name(place.getName())
        .category(place.getCategory())
        .address(place.getAddress())
        .latitude(place.getLatitude())
        .longitude(place.getLongitude())
        .photoUrl(place.getPhotoUrl())
        .distanceToNext(place.getDistanceToNext())
        .movingTip(place.getMovingTip())
        .build();
  }
}
