package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
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

  /**
   * 프론트엔드에서 휠체어 길찾기를 요청하는 엔드포인트 GET
   * /api/v1/routes/wheelchair?startLng=...&startLat=...&endLng=...&endLat=...
   */
  @GetMapping("/wheelchair")
  public ApiResponse<WheelchairRouteResponse> getWheelchairRoute(
      @RequestParam double startLng,
      @RequestParam double startLat,
      @RequestParam double endLng,
      @RequestParam double endLat) {

    WheelchairRouteResponse response =
        orsRouteService.getWheelchairRoute(startLng, startLat, endLng, endLat);

    // 프론트엔드 컨벤션에 맞게 ApiResponse 객체로 감싸서 반환
    return ApiResponse.success(response, "휠체어 맞춤형 경로를 성공적으로 찾았습니다.");
  }

  /** [테스트용] TAGO 도시코드 목록 조회 API 연동 테스트 엔드포인트 GET /api/v1/routes/test/tago */
  @GetMapping("/test/tago")
  public ApiResponse<String> testTagoConnection() {
    String response = tagoRouteService.testTagoCityCodeConnection();
    return ApiResponse.success(response, "TAGO(도시코드) 연동 테스트 성공");
  }

  /**
   * [테스트용] TAGO 정류소별 버스 도착예정정보(저상버스 여부 포함) 연동 테스트 엔드포인트 GET
   * /api/v1/routes/test/tago/bus?cityCode=25&nodeId=DJB8001793
   */
  @GetMapping("/test/tago/bus")
  public ApiResponse<String> testTagoLowFloorBusConnection(
      @RequestParam(defaultValue = "25") int cityCode,
      @RequestParam(defaultValue = "DJB8001793") String nodeId) {

    String response = tagoRouteService.testTagoBusArrivalInfo(cityCode, nodeId);
    return ApiResponse.success(response, "TAGO(저상버스 도착정보) 연동 테스트 성공");
  }

  /**
   * [테스트용] TAGO 좌표기반 근접 정류소 조회 API 연동 테스트 엔드포인트 GET
   * /api/v1/routes/test/tago/station?lat=37.555242&lng=126.972663 (기본값: 방금 ODsay 응답에서 본
   * '서울역버스환승센터(5번승강장)'의 좌표)
   */
  @GetMapping("/test/tago/station")
  public ApiResponse<String> testTagoNearbyStationConnection(
      @RequestParam(defaultValue = "37.555242") double lat,
      @RequestParam(defaultValue = "126.972663") double lng) {

    String response = tagoRouteService.testTagoNearbyStation(lat, lng);
    return ApiResponse.success(response, "TAGO(근접 정류소 조회) 연동 테스트 성공");
  }

  /** [테스트용] ODsay 대중교통 길찾기 API 연동 테스트 엔드포인트 GET /api/v1/routes/test/odsay */
  @GetMapping("/test/odsay")
  public ApiResponse<String> testOdsayConnection(
      @RequestParam(defaultValue = "126.9706069") double startLng, // 기본값: 서울역 경도
      @RequestParam(defaultValue = "37.5546788") double startLat, // 기본값: 서울역 위도
      @RequestParam(defaultValue = "127.0277194") double endLng, // 기본값: 강남역 경도
      @RequestParam(defaultValue = "37.497942") double endLat) { // 기본값: 강남역 위도

    String response = odsayRouteService.testOdsayTransitRoute(startLng, startLat, endLng, endLat);
    return ApiResponse.success(response, "ODsay(대중교통 길찾기) 연동 테스트 성공");
  }
}
