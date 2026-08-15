package com.barrierfree.bf.place.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import java.time.LocalDateTime;
import java.util.List;

public record SavedPlaceResponse(List<SavedPlaceSummary> places) {

  public record SavedPlaceSummary(
      Long id,
      String placeId,
      String name,
      PlaceCategory category,
      String categoryLabel,
      Boolean openNow,
      String address,
      Double latitude,
      Double longitude,
      String photoUrl,
      LocalDateTime createdAt) {}
}
