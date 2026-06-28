package com.barrierfree.bf.route.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import com.barrierfree.bf.route.service.OdsayRouteService;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.route.service.TagoRouteService;
import com.barrierfree.bf.route.service.KakaoMobilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "경로 탐색 (Route)", description = "휠체어 맞춤형 길찾기 및 대중교통(ODsay, TAGO) API 연동 API")
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final OrsRouteService orsRouteService;
    private final TagoRouteService tagoRouteService;
    private final OdsayRouteService odsayRouteService;
    private final KakaoMobilityService kakaoMobilityService;

    /**
     * 프론트엔드에서 휠체어 길찾기를 요청하는 엔드포인트 GET
     * /api/v1/routes/wheelchair?startLng=...&startLat=...&endLng=...&endLat=...
     */
    @Operation(summary = "휠체어 길찾기", description = "ORS 기반 휠체어 맞춤형 경로(계단 회피, 경사도 고려 등)를 탐색합니다.")
    @GetMapping("/wheelchair")
    public ApiResponse<WheelchairRouteResponse> getWheelchairRoute(
            @Parameter(description = "출발지 경도", example = "126.977324") @RequestParam double startLng,
            @Parameter(description = "출발지 위도", example = "37.571407") @RequestParam double startLat,
            @Parameter(description = "도착지 경도", example = "126.977464") @RequestParam double endLng,
            @Parameter(description = "도착지 위도", example = "37.570221") @RequestParam double endLat) {

        WheelchairRouteResponse response =
                orsRouteService.getWheelchairRoute(startLng, startLat, endLng, endLat);

        // 프론트엔드 컨벤션에 맞게 ApiResponse 객체로 감싸서 반환
        return ApiResponse.success(response, "휠체어 맞춤형 경로를 성공적으로 찾았습니다.");
    }

    /** [테스트용] TAGO 도시코드 목록 조회 API 연동 테스트 엔드포인트 GET /api/v1/routes/test/tago */
    @Operation(summary = "[테스트] TAGO 도시코드 조회", description = "TAGO API의 도시코드 목록을 조회하여 연동 상태를 확인합니다.")
    @GetMapping("/test/tago")
    public ApiResponse<String> testTagoConnection() {
        String response = tagoRouteService.testTagoCityCodeConnection();
        return ApiResponse.success(response, "TAGO(도시코드) 연동 테스트 성공");
    }

    /**
     * [테스트용] TAGO 정류소별 버스 도착예정정보(저상버스 여부 포함) 연동 테스트 엔드포인트 GET
     * /api/v1/routes/test/tago/bus?cityCode=25&nodeId=DJB8001793
     */
    @Operation(summary = "[테스트] TAGO 버스 도착예정정보 (저상버스)", description = "특정 정류소의 실시간 버스 도착예정정보 및 저상버스 여부를 조회합니다.")
    @GetMapping("/test/tago/bus")
    public ApiResponse<String> testTagoLowFloorBusConnection(
            @Parameter(description = "도시코드 (예: 25 - 대전)", example = "25") @RequestParam(defaultValue = "25") int cityCode,
            @Parameter(description = "정류소 ID (Node ID)", example = "DJB8001793") @RequestParam(defaultValue = "DJB8001793") String nodeId) {

        String response = tagoRouteService.testTagoBusArrivalInfo(cityCode, nodeId);
        return ApiResponse.success(response, "TAGO(저상버스 도착정보) 연동 테스트 성공");
    }

    /**
     * [테스트용] TAGO 좌표기반 근접 정류소 조회 API 연동 테스트 엔드포인트 GET
     * /api/v1/routes/test/tago/station?lat=37.555242&lng=126.972663 (기본값: 방금 ODsay 응답에서 본
     * '서울역버스환승센터(5번승강장)'의 좌표)
     */
    @Operation(summary = "[테스트] TAGO 근접 정류소 조회", description = "주어진 위/경도 좌표를 기반으로 반경 내의 버스 정류소 목록을 조회합니다.")
    @GetMapping("/test/tago/station")
    public ApiResponse<String> testTagoNearbyStationConnection(
            @Parameter(description = "위도 (기본값: 서울역버스환승센터)", example = "37.555242") @RequestParam(defaultValue = "37.555242") double lat,
            @Parameter(description = "경도", example = "126.972663") @RequestParam(defaultValue = "126.972663") double lng) {

        String response = tagoRouteService.testTagoNearbyStation(lat, lng);
        return ApiResponse.success(response, "TAGO(근접 정류소 조회) 연동 테스트 성공");
    }

    /**
     * [테스트용] ODsay 대중교통 길찾기 API 연동 테스트 엔드포인트
     * 파라미터 확장 (searchPathType, time, opt)
     */
    @Operation(summary = "[테스트] ODsay 대중교통 길찾기", description = "출발시간, 교통수단(버스/지하철) 및 정렬 옵션(OPT)을 지정하여 대중교통 경로를 조회합니다.")
    @GetMapping("/test/odsay")
    public ApiResponse<String> testOdsayConnection(
            @Parameter(description = "출발지 경도", example = "126.9706069") @RequestParam(defaultValue = "126.9706069") double startLng,
            @Parameter(description = "출발지 위도", example = "37.5546788") @RequestParam(defaultValue = "37.5546788") double startLat,
            @Parameter(description = "도착지 경도", example = "127.0277194") @RequestParam(defaultValue = "127.0277194") double endLng,
            @Parameter(description = "도착지 위도", example = "37.497942") @RequestParam(defaultValue = "37.497942") double endLat,
            @Parameter(description = "경로 탐색 옵션 (0: 모두, 1: 지하철만, 2: 버스만)", example = "0") @RequestParam(defaultValue = "0") Integer searchPathType,
            @Parameter(description = "출발 시간 (형식: HH:mm, 빈 값일 경우 현재 시간)", example = "12:00") @RequestParam(required = false) String time,
            @Parameter(description = "경로 정렬 옵션 (0: 최적, 1: 최단시간, 2: 최소환승, 3: 최소도보)", example = "0") @RequestParam(defaultValue = "0") Integer opt) {

        String response =
                odsayRouteService.testOdsayTransitRoute(
                        startLng, startLat, endLng, endLat, searchPathType, time, opt);

        return ApiResponse.success(response, "ODsay(대중교통 길찾기) 연동 테스트 성공");
    }

    /** [테스트용] 카카오 모빌리티 길찾기 연동 테스트 엔드포인트 */
    @Operation(summary = "[테스트] 카카오 자동차 길찾기", description = "카카오 모빌리티 API를 이용한 실시간 자동차 길찾기 테스트입니다.")
    @GetMapping("/test/kakao")
    public ApiResponse<String> testKakaoConnection(
            @Parameter(description = "출발지 (경도,위도)", example = "126.977324,37.571407") @RequestParam String origin,
            @Parameter(description = "도착지 (경도,위도)", example = "127.0277194,37.497942") @RequestParam String destination) {

        String response = kakaoMobilityService.testKakaoDirections(origin, destination);
        return ApiResponse.success(response, "카카오(자동차 길찾기) 연동 테스트 성공");
    }
}