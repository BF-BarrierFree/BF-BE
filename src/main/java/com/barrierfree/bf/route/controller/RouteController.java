package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.barrierfree.bf.route.dto.VehicleRouteResponse;
import com.barrierfree.bf.route.dto.WalkingRouteResponse;
import com.barrierfree.bf.route.service.OdsayRouteService;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.route.service.RouteSearchHistoryService;
import com.barrierfree.bf.route.service.TagoRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Route", description = "경로 탐색 API")
public class RouteController {

  private final OrsRouteService orsRouteService;
  private final TagoRouteService tagoRouteService;
  private final OdsayRouteService odsayRouteService;
  private final RouteSearchHistoryService routeSearchHistoryService;

  @Operation(summary = "도보 경로 탐색", description = "출발지와 도착지 좌표를 기준으로 접근성 도보 경로를 조회합니다.")
  @GetMapping("/walk")
  public ApiResponse<WalkingRouteResponse> getWalkingRoute(
      @Parameter(description = "출발지 경도", example = "126.9706069") @RequestParam double startLng,
      @Parameter(description = "출발지 위도", example = "37.5546788") @RequestParam double startLat,
      @Parameter(description = "도착지 경도", example = "127.0277194") @RequestParam double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942") @RequestParam double endLat) {
    routeSearchHistoryService.save("WALK", startLng, startLat, endLng, endLat);
    WalkingRouteResponse response =
        orsRouteService.getAccessibleWalkingRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "도보 경로를 성공적으로 찾았습니다.");
  }

  @Operation(summary = "차량 경로 탐색", description = "출발지와 도착지 좌표를 기준으로 차량 이동 경로를 조회합니다.")
  @GetMapping("/vehicle")
  public ApiResponse<VehicleRouteResponse> getVehicleRoute(
      @Parameter(description = "출발지 경도", example = "126.9706069") @RequestParam double startLng,
      @Parameter(description = "출발지 위도", example = "37.5546788") @RequestParam double startLat,
      @Parameter(description = "도착지 경도", example = "127.0277194") @RequestParam double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942") @RequestParam double endLat) {
    routeSearchHistoryService.save("VEHICLE", startLng, startLat, endLng, endLat);
    VehicleRouteResponse response =
        orsRouteService.getVehicleRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "차량 경로를 성공적으로 찾았습니다.");
  }

  @Operation(summary = "대중교통 경로 탐색", description = "출발지와 도착지 좌표를 기준으로 대중교통 경로를 조회합니다.")
  @GetMapping("/transit")
  public ApiResponse<TransitRouteResponse> getTransitRoute(
      @Parameter(description = "출발지 경도", example = "126.9706069") @RequestParam double startLng,
      @Parameter(description = "출발지 위도", example = "37.5546788") @RequestParam double startLat,
      @Parameter(description = "도착지 경도", example = "127.0277194") @RequestParam double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942") @RequestParam double endLat) {
    routeSearchHistoryService.save("TRANSIT", startLng, startLat, endLng, endLat);
    TransitRouteResponse response =
        odsayRouteService.getTransitRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "대중교통 경로를 성공적으로 찾았습니다.");
  }

  @Operation(summary = "TAGO 연동 테스트", description = "TAGO 시외버스 API 연결 상태를 테스트합니다.")
  @GetMapping("/test/tago")
  public ApiResponse<String> testTagoConnection() {
    String response = tagoRouteService.testTagoCityCodeConnection();
    return ApiResponse.success(response, "TAGO(시외버스) 연동 테스트 성공");
  }

  @Operation(summary = "TAGO 저상버스 도착 정보 테스트", description = "도시 코드와 정류소 ID로 저상버스 도착 정보를 테스트합니다.")
  @GetMapping("/test/tago/bus")
  public ApiResponse<String> testTagoLowFloorBusConnection(
      @Parameter(description = "도시 코드", example = "25") @RequestParam(defaultValue = "25")
          int cityCode,
      @Parameter(description = "정류소 ID", example = "DJB8001793")
          @RequestParam(defaultValue = "DJB8001793")
          String nodeId) {
    String response = tagoRouteService.testTagoBusArrivalInfo(cityCode, nodeId);
    return ApiResponse.success(response, "TAGO(저상버스 도착정보) 연동 테스트 성공");
  }

  @Operation(summary = "TAGO 주변 정류소 테스트", description = "좌표를 기준으로 주변 정류소 조회를 테스트합니다.")
  @GetMapping("/test/tago/station")
  public ApiResponse<String> testTagoNearbyStationConnection(
      @Parameter(description = "위도", example = "37.555242")
          @RequestParam(defaultValue = "37.555242")
          double lat,
      @Parameter(description = "경도", example = "126.972663")
          @RequestParam(defaultValue = "126.972663")
          double lng) {
    String response = tagoRouteService.testTagoNearbyStation(lat, lng);
    return ApiResponse.success(response, "TAGO(정류장 조회) 연동 테스트 성공");
  }

  @Operation(summary = "ODsay 연동 테스트", description = "좌표를 기준으로 ODsay 대중교통 경로 API 연결을 테스트합니다.")
  @GetMapping("/test/odsay")
  public ApiResponse<String> testOdsayConnection(
      @Parameter(description = "출발지 경도", example = "126.9706069")
          @RequestParam(defaultValue = "126.9706069")
          double startLng,
      @Parameter(description = "출발지 위도", example = "37.5546788")
          @RequestParam(defaultValue = "37.5546788")
          double startLat,
      @Parameter(description = "도착지 경도", example = "127.0277194")
          @RequestParam(defaultValue = "127.0277194")
          double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942")
          @RequestParam(defaultValue = "37.497942")
          double endLat) {
    String response = odsayRouteService.testOdsayTransitRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "ODsay 연동 테스트 성공");
  }
}
