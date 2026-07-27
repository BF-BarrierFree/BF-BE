package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.barrierfree.bf.route.dto.VehicleRouteResponse;
import com.barrierfree.bf.route.dto.WalkingRouteResponse;
import com.barrierfree.bf.route.service.OdsayRouteService;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.route.service.TagoRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

  private final OrsRouteService orsRouteService;
  private final TagoRouteService tagoRouteService;
  private final OdsayRouteService odsayRouteService;

  @GetMapping("/walk")
  public ApiResponse<WalkingRouteResponse> getWalkingRoute(
      @RequestParam double startLng,
      @RequestParam double startLat,
      @RequestParam double endLng,
      @RequestParam double endLat) {
    WalkingRouteResponse response =
        orsRouteService.getAccessibleWalkingRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "도보 경로를 성공적으로 찾았습니다.");
  }

  @GetMapping("/vehicle")
  public ApiResponse<VehicleRouteResponse> getVehicleRoute(
      @RequestParam double startLng,
      @RequestParam double startLat,
      @RequestParam double endLng,
      @RequestParam double endLat) {
    VehicleRouteResponse response =
        orsRouteService.getVehicleRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "차량 경로를 성공적으로 찾았습니다.");
  }

  @GetMapping("/transit")
  public ApiResponse<TransitRouteResponse> getTransitRoute(
      @RequestParam double startLng,
      @RequestParam double startLat,
      @RequestParam double endLng,
      @RequestParam double endLat) {
    TransitRouteResponse response =
        odsayRouteService.getTransitRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "대중교통 경로를 성공적으로 찾았습니다.");
  }

  @GetMapping("/test/tago")
  public ApiResponse<String> testTagoConnection() {
    String response = tagoRouteService.testTagoCityCodeConnection();
    return ApiResponse.success(response, "TAGO(시외버스) 연동 테스트 성공");
  }

  @GetMapping("/test/tago/bus")
  public ApiResponse<String> testTagoLowFloorBusConnection(
      @RequestParam(defaultValue = "25") int cityCode,
      @RequestParam(defaultValue = "DJB8001793") String nodeId) {
    String response = tagoRouteService.testTagoBusArrivalInfo(cityCode, nodeId);
    return ApiResponse.success(response, "TAGO(저상버스 도착정보) 연동 테스트 성공");
  }

  @GetMapping("/test/tago/station")
  public ApiResponse<String> testTagoNearbyStationConnection(
      @RequestParam(defaultValue = "37.555242") double lat,
      @RequestParam(defaultValue = "126.972663") double lng) {
    String response = tagoRouteService.testTagoNearbyStation(lat, lng);
    return ApiResponse.success(response, "TAGO(정류장 조회) 연동 테스트 성공");
  }

  @GetMapping("/test/odsay")
  public ApiResponse<String> testOdsayConnection(
      @RequestParam(defaultValue = "126.9706069") double startLng,
      @RequestParam(defaultValue = "37.5546788") double startLat,
      @RequestParam(defaultValue = "127.0277194") double endLng,
      @RequestParam(defaultValue = "37.497942") double endLat) {
    String response = odsayRouteService.testOdsayTransitRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "ODsay 연동 테스트 성공");
  }
}
