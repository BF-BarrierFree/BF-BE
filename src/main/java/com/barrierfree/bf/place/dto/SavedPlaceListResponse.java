package com.barrierfree.bf.place.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SavedPlaceListResponse(List<SavedPlaceListSummary> lists) {

  public record SavedPlaceListSummary(Long id, String name, LocalDateTime createdAt) {}
}
