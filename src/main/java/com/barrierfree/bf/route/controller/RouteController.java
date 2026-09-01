package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.barrierfree.bf.route.dto.VehicleRouteResponse;
import com.barrierfree.bf.route.dto.WalkingRouteResponse;
import com.barrierfree.bf.route.service.KakaoMobilityService;
import com.barrierfree.bf.route.service.OdsayRouteService;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.route.service.RouteSearchHistoryService;
import com.barrierfree.bf.route.service.TagoRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
  private final KakaoMobilityService kakaoMobilityService;
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
    return ApiResponse.success(
        tagoRouteService.testTagoCityCodeConnection(), "TAGO(시외버스) 연동 테스트 성공");
  }

  @Operation(summary = "TAGO 저상버스 도착 정보 테스트", description = "도시 코드와 정류소 ID로 저상버스 도착 정보를 테스트합니다.")
  @GetMapping("/test/tago/bus")
  public ApiResponse<String> testTagoLowFloorBusConnection(
      @Parameter(description = "도시 코드", example = "25") @RequestParam(defaultValue = "25")
          int cityCode,
      @Parameter(description = "정류소 ID", example = "DJB8001793")
          @RequestParam(defaultValue = "DJB8001793")
          String nodeId) {
    return ApiResponse.success(
        tagoRouteService.testTagoBusArrivalInfo(cityCode, nodeId), "TAGO(저상버스 도착정보) 연동 테스트 성공");
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
    return ApiResponse.success(
        tagoRouteService.testTagoNearbyStation(lat, lng), "TAGO(정류장 조회) 연동 테스트 성공");
  }

  @Operation(summary = "ODsay 연동 테스트", description = "좌표와 옵션을 기준으로 ODsay 대중교통 경로 API 연결을 테스트합니다.")
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
          double endLat,
      @Parameter(description = "경로 탐색 옵션 (0: 모두, 1: 지하철만, 2: 버스만)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer searchPathType,
      @Parameter(description = "출발 시각 (형식: HH:mm, 비어 있으면 현재 시각)", example = "12:00")
          @RequestParam(required = false)
          String time,
      @Parameter(description = "경로 정렬 옵션 (0: 최적, 1: 최소시간, 2: 최소환승, 3: 최소보행)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer opt) {
    return ApiResponse.success(
        odsayRouteService.testOdsayTransitRoute(
            startLng, startLat, endLng, endLat, searchPathType, time, opt),
        "ODsay 연동 테스트 성공");
  }

  @Operation(summary = "Kakao 차량 경로 테스트", description = "현재 교통상황 기준 차량 경로와 요금을 테스트합니다.")
  @GetMapping("/test/kakao")
  public ApiResponse<String> testKakaoConnection(
      @Parameter(description = "출발지 경도", example = "126.977324")
          @RequestParam(defaultValue = "126.977324")
          double startLng,
      @Parameter(description = "출발지 위도", example = "37.571407")
          @RequestParam(defaultValue = "37.571407")
          double startLat,
      @Parameter(description = "도착지 경도", example = "127.027715")
          @RequestParam(defaultValue = "127.027715")
          double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942")
          @RequestParam(defaultValue = "37.497942")
          double endLat,
      @Parameter(description = "우선순위 (RECOMMEND, TIME, DISTANCE)", example = "RECOMMEND")
          @RequestParam(defaultValue = "RECOMMEND")
          String priority,
      @Parameter(description = "대안 경로 포함 여부", example = "false")
          @RequestParam(defaultValue = "false")
          Boolean alternatives,
      @Parameter(description = "회피 옵션", example = "toll") @RequestParam(required = false)
          List<String> avoid,
      @Parameter(description = "교통 이벤트 반영 옵션", example = "0") @RequestParam(defaultValue = "0")
          Integer roadevent) {
    String origin = startLng + "," + startLat;
    String destination = endLng + "," + endLat;
    return ApiResponse.success(
        kakaoMobilityService.testKakaoDirections(
            origin, destination, priority, alternatives, avoid, roadevent),
        "Kakao 차량 경로 테스트 성공");
  }

  @Operation(summary = "Kakao 미래 차량 경로 테스트", description = "미래 출발 시각 기준 차량 경로와 ETA를 테스트합니다.")
  @GetMapping("/test/kakao/future")
  public ApiResponse<String> testKakaoFutureConnection(
      @Parameter(description = "출발지 경도", example = "126.977324")
          @RequestParam(defaultValue = "126.977324")
          double startLng,
      @Parameter(description = "출발지 위도", example = "37.571407")
          @RequestParam(defaultValue = "37.571407")
          double startLat,
      @Parameter(description = "도착지 경도", example = "127.027715")
          @RequestParam(defaultValue = "127.027715")
          double endLng,
      @Parameter(description = "도착지 위도", example = "37.497942")
          @RequestParam(defaultValue = "37.497942")
          double endLat,
      @Parameter(description = "출발 시각 (형식: YYYYMMDDHHMM)", example = "202606290830") @RequestParam
          String departureTime,
      @Parameter(description = "우선순위 (RECOMMEND, TIME, DISTANCE)", example = "RECOMMEND")
          @RequestParam(defaultValue = "RECOMMEND")
          String priority,
      @Parameter(description = "대안 경로 포함 여부", example = "false")
          @RequestParam(defaultValue = "false")
          Boolean alternatives,
      @Parameter(description = "회피 옵션", example = "toll") @RequestParam(required = false)
          List<String> avoid,
      @Parameter(description = "교통 이벤트 반영 옵션", example = "0") @RequestParam(defaultValue = "0")
          Integer roadevent) {
    String origin = startLng + "," + startLat;
    String destination = endLng + "," + endLat;
    return ApiResponse.success(
        kakaoMobilityService.testKakaoFutureDirections(
            origin, destination, departureTime, priority, alternatives, avoid, roadevent),
        "Kakao 미래 차량 경로 테스트 성공");
  }
}
