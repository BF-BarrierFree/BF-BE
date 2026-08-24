package com.barrierfree.bf.route.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record OrsRouteRequest(
    List<List<Double>> coordinates,
    boolean elevation,
    Map<String, List<String>> options,
    @JsonProperty("extra_info") List<String> extraInfo) {

  public static OrsRouteRequest createWheelchair(
      double startLng, double startLat, double endLng, double endLat) {
    return create(
        startLng,
        startLat,
        endLng,
        endLat,
        true,
        Map.of("avoid_features", List.of("steps")),
        List.of("surface"));
  }

  public static OrsRouteRequest createWalking(
      double startLng, double startLat, double endLng, double endLat) {
    return create(startLng, startLat, endLng, endLat, false, Map.of(), List.of("surface"));
  }

  public static OrsRouteRequest createDriving(
      double startLng, double startLat, double endLng, double endLat) {
    return create(startLng, startLat, endLng, endLat, false, Map.of(), List.of("surface"));
  }

  private static OrsRouteRequest create(
      double startLng,
      double startLat,
      double endLng,
      double endLat,
      boolean elevation,
      Map<String, List<String>> options,
      List<String> extraInfo) {
    return new OrsRouteRequest(
        List.of(List.of(startLng, startLat), List.of(endLng, endLat)),
        elevation,
        options,
        extraInfo);
  }
}
