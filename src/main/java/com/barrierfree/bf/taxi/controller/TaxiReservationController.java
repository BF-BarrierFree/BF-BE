package com.barrierfree.bf.taxi.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.taxi.batch.TaxiCenterBatchService;
import com.barrierfree.bf.taxi.dto.TaxiCenterResponse;
import com.barrierfree.bf.taxi.dto.TaxiReservationRequest;
import com.barrierfree.bf.taxi.service.TaxiReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "장애인 콜택시", description = "장애인 콜택시 센터 조회, 요금 산출 및 예약 API")
@RestController
@RequestMapping("/api/v1/taxis")
@RequiredArgsConstructor
public class TaxiReservationController {

  private final TaxiReservationService taxiReservationService;
  private final TaxiCenterBatchService taxiCenterBatchService; // 수동 동기화를 위해 주입

  @Operation(
      summary = "출발지 기반 가장 가까운 센터 리스트 조회",
      description = "사용자의 출발지(위/경도)를 기준으로 반경 내 가장 가까운 택시 센터 정보를 조회합니다.")
  @GetMapping("/centers/nearby")
  public ApiResponse<List<TaxiCenterResponse>> getNearestCenters(
      @Parameter(description = "출발지 위도", example = "37.5665") @RequestParam double lat,
      @Parameter(description = "출발지 경도", example = "126.9780") @RequestParam double lng) {

    // 반경 30km 이내, 최대 3개의 센터를 추천합니다.
    List<TaxiCenterResponse> centers = taxiReservationService.findNearestCenters(lat, lng, 30.0, 3);
    return ApiResponse.success(centers);
  }

  @Operation(
      summary = "선택한 센터 규정 기반 예상 요금 산출",
      description = "LLM을 연동하여 출발/도착지 및 이동 거리와 센터 규정을 기반으로 예상 요금을 계산합니다.")
  @PostMapping("/fare")
  public ApiResponse<String> estimateFare(
      @RequestBody TaxiReservationRequest.FareEstimate request) {
    String estimatedFare = taxiReservationService.estimateFare(request);
    return ApiResponse.success(estimatedFare, "예상 요금 산출에 성공했습니다.");
  }

  @Operation(summary = "예약 이력 저장", description = "사용자가 최종적으로 시도한 예약 이력을 데이터베이스에 저장합니다.")
  @PostMapping("/reservations")
  public ApiResponse<?> createReservationHistory(
      @Parameter(description = "사용자 ID", example = "1") @RequestParam
          Long userId, // 실무에서는 @AuthenticationPrincipal이나 AuthUser 어노테이션 등으로 대체
      @RequestBody TaxiReservationRequest.CreateReservation request) {

    taxiReservationService.saveReservationHistory(userId, request);
    return ApiResponse.successWithNoContent();
  }

  @Operation(
      summary = "[관리자용] 콜택시 센터 공공데이터 수동 적재/갱신",
      description = "스케줄러를 기다리지 않고 공공데이터 API를 호출하여 최신 센터 정보를 즉시 DB에 갱신합니다.")
  @PostMapping("/centers/sync")
  public ApiResponse<?> syncTaxiCentersManually() {
    taxiCenterBatchService.fetchAndUpsertTaxiCenters();
    return ApiResponse.success(null, "센터 데이터 수동 갱신(배치) 작업이 완료되었습니다.");
  }
}
