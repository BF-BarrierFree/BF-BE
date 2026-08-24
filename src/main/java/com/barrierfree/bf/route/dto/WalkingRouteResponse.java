package com.barrierfree.bf.route.dto;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.List;

public record WalkingRouteResponse(
    String routeMode,
    boolean wheelchairAccessible,
    double totalDistanceMeter,
    double totalDurationSecond,
    List<Point> pathCoordinates,
    List<RouteSurfaceSegment> surfaceSegments) {

  public record Point(double lng, double lat, double elevation) {}

  public static WalkingRouteResponse from(
      WheelchairRouteResponse route, boolean wheelchairAccessible, String routeMode) {
    if (route == null || route.pathCoordinates() == null || route.pathCoordinates().isEmpty()) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    List<Point> points =
        route.pathCoordinates().stream()
            .map(point -> new Point(point.lng(), point.lat(), point.elevation()))
            .toList();

    return new WalkingRouteResponse(
        routeMode,
        wheelchairAccessible,
        route.totalDistanceMeter(),
        route.totalDurationSecond(),
        points,
        route.surfaceSegments());
  }
}
