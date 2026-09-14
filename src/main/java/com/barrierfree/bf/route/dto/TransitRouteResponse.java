package com.barrierfree.bf.route.dto;

import java.util.List;

public record TransitRouteResponse(
    int routeCount, Integer totalTimeMinute, Integer totalDistanceMeter, List<RouteOption> routes) {

  public record RouteOption(
      Integer pathType,
      Integer totalTimeMinute,
      Integer totalDistanceMeter,
      Integer totalWalkMeter,
      Integer paymentWon,
      Integer transferCount,
      String lastEndStation,
      String estimatedArrivalTime,
      List<Point> pathCoordinates,
      List<Segment> segments) {}

  public record Segment(
      Integer trafficType,
      String startName,
      String endName,
      Integer distanceMeter,
      Integer sectionTimeMinute,
      Integer stationCount,
      Double startLng,
      Double startLat,
      Double endLng,
      Double endLat,
      List<String> laneNames,
      List<Lane> lanes,
      List<Stop> passStops,
      List<Point> pathCoordinates,
      boolean realtimeAvailable,
      List<RealtimeArrival> realtimeArrivals,
      List<BusLocation> busLocations) {}

  public record Lane(
      String name,
      String busNo,
      Integer type,
      String typeName,
      String busId,
      Integer subwayCode,
      String subwayCityCode) {}

  public record Stop(Integer index, String stationId, String stationName, Double lng, Double lat) {}

  public record Point(Double lng, Double lat) {}

  public record RealtimeArrival(
      String busNo,
      Integer arrivalMinutes,
      String arrivalMessage,
      Integer remainingStops,
      String vehicleType,
      String currentLocation) {}

  public record BusLocation(String busNo, Double lng, Double lat, String stationName) {}
}
