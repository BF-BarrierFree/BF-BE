package com.barrierfree.bf.place.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import java.util.List;

public record PlaceAutocompleteResponse(List<Suggestion> suggestions) {

  public record Suggestion(
      String placeId,
      String name,
      String description,
      String secondaryText,
      PlaceCategory category,
      String categoryLabel) {}
}
