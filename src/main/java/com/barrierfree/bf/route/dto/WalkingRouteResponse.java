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

  public record Point(double lng, double lat) {}

  public static WalkingRouteResponse from(OrsGeoJsonResponse rawResponse) {
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

    List<Point> points =
        firstFeature.geometry().coordinates().stream()
            .map(
                coord -> {
                  if (coord == null || coord.size() < 2) {
                    throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
                  }
                  return new Point(coord.get(0), coord.get(1));
                })
            .toList();

    return new WalkingRouteResponse(
        "WALKING",
        false,
        summary.distance(),
        summary.duration(),
        points,
        extractSurfaceSegments(firstFeature));
  }

  private static List<RouteSurfaceSegment> extractSurfaceSegments(
      OrsGeoJsonResponse.Feature feature) {
    if (feature == null
        || feature.properties() == null
        || feature.properties().extras() == null
        || feature.properties().extras().surface() == null
        || feature.properties().extras().surface().values() == null) {
      return List.of();
    }

    return feature.properties().extras().surface().values().stream()
        .filter(value -> value != null && value.size() >= 3)
        .map(
            value ->
                new RouteSurfaceSegment(
                    value.get(0), value.get(1), value.get(2), surfaceLabel(value.get(2))))
        .toList();
  }

  private static String surfaceLabel(Integer surfaceCode) {
    if (surfaceCode == null) {
      return "UNKNOWN";
    }
    return switch (surfaceCode) {
      case 1 -> "PAVED";
      case 2 -> "UNPAVED";
      case 3 -> "ASPHALT";
      case 4 -> "CONCRETE";
      case 5 -> "COBBLESTONE";
      case 8 -> "COMPACTED_GRAVEL";
      case 10 -> "GRAVEL";
      case 11 -> "DIRT";
      case 14 -> "PAVING_STONES";
      default -> "UNKNOWN";
    };
  }
}
