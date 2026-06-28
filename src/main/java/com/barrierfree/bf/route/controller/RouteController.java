package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import com.barrierfree.bf.route.service.OrsRouteService;
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
}
