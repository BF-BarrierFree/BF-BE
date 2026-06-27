package com.barrierfree.bf.route.dto;

import java.util.List;

/** ORS API에서 반환하는 방대한 양의 GeoJSON 원본 데이터를 파싱하기 위한 DTO 필요한 핵심 노드(거리, 시간, 좌표 배열)만 매핑합니다. */
public record OrsGeoJsonResponse(List<Feature> features) {

  public record Feature(Geometry geometry, Properties properties) {}

  public record Geometry(List<List<Double>> coordinates // [ [lng, lat, elevation], ... ]
      ) {}

  public record Properties(Summary summary) {}

  public record Summary(
      double distance, // 총 미터(m)
      double duration // 총 초(s)
      ) {}
}
