package com.barrierfree.bf.route.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RouteSearchHistoryJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  @PostConstruct
  void createTableIfMissing() {
    jdbcTemplate.execute(
        """
        create table if not exists route_search_history (
          id bigserial primary key,
          route_type varchar(30) not null,
          start_lng double precision not null,
          start_lat double precision not null,
          end_lng double precision not null,
          end_lat double precision not null,
          created_at timestamp not null default current_timestamp
        )
        """);
    jdbcTemplate.execute(
        """
        create index if not exists idx_route_search_history_created_at
        on route_search_history (created_at desc)
        """);
  }

  public void save(
      String routeType, Double startLng, Double startLat, Double endLng, Double endLat) {
    jdbcTemplate.update(
        """
        insert into route_search_history
          (route_type, start_lng, start_lat, end_lng, end_lat)
        values (?, ?, ?, ?, ?)
        """,
        routeType,
        startLng,
        startLat,
        endLng,
        endLat);
  }
}
