package com.barrierfree.bf.place.repository;

import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchHistoryResponse;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceSearchHistoryJdbcRepository {

  private final JdbcTemplate jdbcTemplate;

  @PostConstruct
  void createTableIfMissing() {
    jdbcTemplate.execute(
        """
        create table if not exists place_search_history (
          id bigserial primary key,
          keyword varchar(255) not null,
          category varchar(50) not null,
          latitude double precision,
          longitude double precision,
          radius integer,
          created_at timestamp not null default current_timestamp
        )
        """);
    jdbcTemplate.execute(
        """
        create index if not exists idx_place_search_history_created_at
        on place_search_history (created_at desc)
        """);
  }

  public void save(
      String keyword, PlaceCategory category, Double latitude, Double longitude, Integer radius) {
    jdbcTemplate.update(
        """
        insert into place_search_history (keyword, category, latitude, longitude, radius)
        values (?, ?, ?, ?, ?)
        """,
        keyword,
        category.name(),
        latitude,
        longitude,
        radius);
  }

  public List<PlaceSearchHistoryResponse.SearchHistory> findRecent(int size) {
    return jdbcTemplate.query(
        """
        select id, keyword, category, latitude, longitude, radius, created_at
        from place_search_history
        order by created_at desc
        limit ?
        """,
        this::mapRow,
        size);
  }

  private PlaceSearchHistoryResponse.SearchHistory mapRow(ResultSet rs, int rowNum)
      throws SQLException {
    PlaceCategory category = PlaceCategory.from(rs.getString("category"));
    return new PlaceSearchHistoryResponse.SearchHistory(
        rs.getLong("id"),
        rs.getString("keyword"),
        category,
        category.getLabel(),
        getNullableDouble(rs, "latitude"),
        getNullableDouble(rs, "longitude"),
        getNullableInteger(rs, "radius"),
        rs.getObject("created_at", LocalDateTime.class));
  }

  private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
    double value = rs.getDouble(column);
    return rs.wasNull() ? null : value;
  }

  private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
