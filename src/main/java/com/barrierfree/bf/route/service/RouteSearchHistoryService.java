package com.barrierfree.bf.route.service;

import com.barrierfree.bf.route.repository.RouteSearchHistoryJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteSearchHistoryService {

  private final RouteSearchHistoryJdbcRepository repository;

  public void save(
      String routeType,
      Double startLng,
      Double startLat,
      Double endLng,
      Double endLat) {
    repository.save(routeType, startLng, startLat, endLng, endLat);
  }
}
