package com.barrierfree.bf.route.dto;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.service.RoutePricingService;
import java.util.List;

public record VehicleRouteResponse(
    String routeMode,
    String regionCode,
    String regionLabel,
    double totalDistanceMeter,
    double totalDurationSecond,
    List<Point> pathCoordinates,
    FareEstimate taxiFare,
    FareEstimate accessibleTaxiFare,
    List<RouteSurfaceSegment> surfaceSegments) {

  public record Point(double lng, double lat, double elevation) {}

  public record FareEstimate(
      int baseFareWon,
      int estimatedFareWon,
      int estimatedWaitMinutes,
      String providerCategory,
      String note) {}

  public static VehicleRouteResponse from(
      WheelchairRouteResponse route,
      RoutePricingService.Region region,
      FareEstimate taxiFare,
      FareEstimate accessibleTaxiFare) {
    if (route == null || route.pathCoordinates() == null || route.pathCoordinates().isEmpty()) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    List<Point> points =
        route.pathCoordinates().stream()
            .map(point -> new Point(point.lng(), point.lat(), point.elevation()))
            .toList();

    return new VehicleRouteResponse(
        "DRIVING_CAR",
        region.code(),
        region.label(),
        route.totalDistanceMeter(),
        route.totalDurationSecond(),
        points,
        taxiFare,
        accessibleTaxiFare,
        route.surfaceSegments());
  }
}
