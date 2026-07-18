package com.barrierfree.bf.place.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PlaceSearchHistoryResponse(List<SearchHistory> histories) {

  public record SearchHistory(
      Long id,
      String keyword,
      PlaceCategory category,
      String categoryLabel,
      Double latitude,
      Double longitude,
      Integer radius,
      LocalDateTime searchedAt) {}
}
