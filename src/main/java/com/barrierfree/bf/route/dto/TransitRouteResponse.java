package com.barrierfree.bf.route.dto;

import java.util.List;

public record TransitRouteResponse(int routeCount, List<RouteOption> routes) {

  public record RouteOption(
      Integer pathType,
      Integer totalTimeMinute,
      Integer totalDistanceMeter,
      Integer paymentWon,
      Integer transferCount,
      List<Segment> segments) {}

  public record Segment(
      Integer trafficType,
      String startName,
      String endName,
      Integer distanceMeter,
      Integer sectionTimeMinute,
      List<String> laneNames) {}
}
