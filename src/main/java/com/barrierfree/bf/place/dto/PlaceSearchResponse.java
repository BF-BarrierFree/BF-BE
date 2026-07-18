package com.barrierfree.bf.place.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import java.util.List;

public record PlaceSearchResponse(List<PlaceSummary> places, String nextPageToken, Boolean hasNext) {

  public record PlaceSummary(
      String placeId,
      String name,
      String address,
      Double latitude,
      Double longitude,
      PlaceCategory category,
      String categoryLabel,
      Boolean wheelchairAccessibleEntrance,
      Boolean wheelchairAccessibleParking,
      Boolean wheelchairAccessibleRestroom,
      Boolean wheelchairAccessibleSeating) {}
}
