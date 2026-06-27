package com.barrierfree.bf.route.dto;

import java.util.List;
import java.util.Map;

/** ORS 서버로 전송할 휠체어 길찾기 요청 규격 */
public record OrsRouteRequest(
    List<List<Double>> coordinates, boolean elevation, Map<String, List<String>> options) {
  /** 휠체어 전용 요청(계단 회피 옵션 포함)을 생성하는 팩토리 메서드 */
  public static OrsRouteRequest createWheelchair(
      double startLng, double startLat, double endLng, double endLat) {
    return new OrsRouteRequest(
        List.of(List.of(startLng, startLat), List.of(endLng, endLat)),
        true, // 고도(경사도) 데이터 포함 요청
        Map.of("avoid_features", List.of("steps")) // 계단 회피 필수
        );
  }
}
