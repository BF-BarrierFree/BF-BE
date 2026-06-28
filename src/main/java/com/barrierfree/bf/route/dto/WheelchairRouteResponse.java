package com.barrierfree.bf.route.dto;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.List;

/** 프론트엔드(FE)에 최종적으로 반환할 가벼운 응답 DTO 무거운 원본 GeoJSON에서 Polyline을 그리기 위한 핵심 데이터만 추출합니다. */
public record WheelchairRouteResponse(
    double totalDistanceMeter, double totalDurationSecond, List<Point> pathCoordinates) {
  /** 프론트엔드용 맵핑을 위한 내부 레코드 (경도, 위도, 고도) */
  public record Point(double lng, double lat, double elevation) {}

  /** 원본 ORS GeoJSON 응답을 FE 맞춤형 DTO로 변환하는 팩토리 메서드 */
  public static WheelchairRouteResponse from(OrsGeoJsonResponse rawResponse) {
    // Null/empty 체크: 핵심 경로 데이터가 없으면 도메인 예외 발생
    if (rawResponse == null || rawResponse.features() == null || rawResponse.features().isEmpty()) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    OrsGeoJsonResponse.Feature firstFeature = rawResponse.features().get(0);
    if (firstFeature == null || firstFeature.properties() == null) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    OrsGeoJsonResponse.Summary summary = firstFeature.properties().summary();
    if (summary == null) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    if (firstFeature.geometry() == null
        || firstFeature.geometry().coordinates() == null
        || firstFeature.geometry().coordinates().isEmpty()) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    // 3D 좌표 배열(경도, 위도, 고도)을 Point 객체 리스트로 매핑
    // 각 좌표는 최소 2개 값(경도, 위도)을 가져야 하며, 그렇지 않으면 ROUTE_NOT_FOUND 처리
    List<Point> points =
        firstFeature.geometry().coordinates().stream()
            .map(
                coord -> {
                  if (coord == null || coord.size() < 2) {
                    throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
                  }
                  return new Point(coord.get(0), coord.get(1), coord.size() > 2 ? coord.get(2) : 0.0);
                })
            .toList();

    return new WheelchairRouteResponse(summary.distance(), summary.duration(), points);
  }
}
