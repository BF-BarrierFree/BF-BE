package com.barrierfree.bf.place.service;

import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchHistoryResponse;
import com.barrierfree.bf.place.repository.PlaceSearchHistoryJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchHistoryService {

  private static final int DEFAULT_HISTORY_SIZE = 10;
  private static final int MAX_HISTORY_SIZE = 50;

  private final PlaceSearchHistoryJdbcRepository repository;

  public void save(
      String keyword, PlaceCategory category, Double latitude, Double longitude, Integer radius) {
    repository.save(keyword, category, latitude, longitude, radius);
  }

  public PlaceSearchHistoryResponse getRecent(Integer size) {
    int normalizedSize = normalizeSize(size);
    return new PlaceSearchHistoryResponse(repository.findRecent(normalizedSize));
  }

  private int normalizeSize(Integer size) {
    if (size == null) {
      return DEFAULT_HISTORY_SIZE;
    }
    if (size < 1) {
      return DEFAULT_HISTORY_SIZE;
    }
    return Math.min(size, MAX_HISTORY_SIZE);
  }
}
