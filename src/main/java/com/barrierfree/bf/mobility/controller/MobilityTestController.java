package com.barrierfree.bf.mobility.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.mobility.dto.VehicleOperationResponse;
import com.barrierfree.bf.mobility.service.MobilityTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.mobility.dto.CenterInfoResponse;
import com.barrierfree.bf.mobility.dto.VehicleInfoResponse;
import com.barrierfree.bf.mobility.dto.VehicleOperationResponse;
import com.barrierfree.bf.mobility.dto.VehicleUseResponse;
import com.barrierfree.bf.mobility.service.MobilityTestService;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mobility Test API", description = "교통약자 이동지원 외부 API 검증용 컨트롤러")
@RestController
@RequestMapping("/api/v1/test/mobility")
@RequiredArgsConstructor
@Profile("!prod")
@Validated
public class MobilityTestController {

    private final MobilityTestService mobilityTestService;

    @Operation(summary = "[Step 1] 차량 실시간 운행이력 API 테스트", description = "행정안전부 API에 직접 통신하여 실제 데이터 포맷을 검증합니다.")
    @GetMapping("/operation")
    public ApiResponse<List<VehicleOperationResponse>> testVehicleOperation(
        @Parameter(description = "지자체 법정동코드 (10자리)", example = "4311000000")
        @Pattern(regexp = "^\\d{10}$", message = "지자체 법정동코드는 10자리 숫자여야 합니다.")
        @RequestParam(required = false) String stdgCd,

        @Parameter(description = "조회 시작일 (YYYYMMDD 8자리)", example = "20260601")
        @Pattern(regexp = "^\\d{8}$", message = "조회 시작일은 YYYYMMDD 형식의 8자리 숫자여야 합니다.")
        @RequestParam(required = false) String fromDate,

        @Parameter(description = "조회 종료일 (YYYYMMDD 8자리)", example = "20260629")
        @Pattern(regexp = "^\\d{8}$", message = "조회 종료일은 YYYYMMDD 형식의 8자리 숫자여야 합니다.")
        @RequestParam(required = false) String toDate
    ) {
        String validStdgCd = (stdgCd != null) ? stdgCd : "4311000000";
        String validFromDate = (fromDate != null) ? fromDate : LocalDate.now().minusDays(28).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String validToDate = (toDate != null) ? toDate : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<VehicleOperationResponse> response =
            mobilityTestService.getVehicleOperationHistory(validStdgCd, validFromDate, validToDate);

        return ApiResponse.success(response, "차량 운행이력 조회에 성공하였습니다.");
    }

    @Operation(summary = "[Step 2] 센터 현황 정보 API 테스트", description = "지자체별 센터 전화번호, 이용요금 등 상세 규정을 검증합니다.")
    @GetMapping("/center")
    public ApiResponse<List<CenterInfoResponse>> testCenterInfo(
        @Parameter(description = "지자체 법정동코드 (10자리)", example = "4311000000")
        @Pattern(regexp = "^\\d{10}$", message = "지자체 법정동코드는 10자리 숫자여야 합니다.")
        @RequestParam(defaultValue = "4311000000") String stdgCd
    ) {
        List<CenterInfoResponse> response = mobilityTestService.getCenterInfo(stdgCd);
        return ApiResponse.success(response, "센터 현황 조회에 성공하였습니다.");
    }

    @Operation(summary = "[Step 2] 실시간 대기 인원 현황 API 테스트", description = "현재 비어있는 배차 가능 차량 수와 대기 인원 정보를 검증합니다.")
    @GetMapping("/vehicle/use")
    public ApiResponse<List<VehicleUseResponse>> testVehicleUseInfo(
        @Parameter(description = "지자체 법정동코드 (10자리)", example = "4311000000")
        @Pattern(regexp = "^\\d{10}$", message = "지자체 법정동코드는 10자리 숫자여야 합니다.")
        @RequestParam(defaultValue = "4311000000") String stdgCd
    ) {
        List<VehicleUseResponse> response = mobilityTestService.getVehicleUseInfo(stdgCd);
        return ApiResponse.success(response, "실시간 이용 현황 조회에 성공하였습니다.");
    }

    @Operation(summary = "[Step 2] 차량 기본 스펙 API 테스트", description = "차량 모델명과 휠체어 수용 개수 등 제원 정보를 검증합니다.")
    @GetMapping("/vehicle/info")
    public ApiResponse<List<VehicleInfoResponse>> testVehicleInfo(
        @Parameter(description = "지자체 법정동코드 (10자리)", example = "4311000000")
        @Pattern(regexp = "^\\d{10}$", message = "지자체 법정동코드는 10자리 숫자여야 합니다.")
        @RequestParam(defaultValue = "4311000000") String stdgCd
    ) {
        List<VehicleInfoResponse> response = mobilityTestService.getVehicleInfo(stdgCd);
        return ApiResponse.success(response, "차량 기본 정보 조회에 성공하였습니다.");
    }

    @Operation(summary = "[Step 3] 전국 센터 현황 정보 전체 조회", description = "전국 모든 지자체의 센터 정보 리스트를 한 번에 불러옵니다 (추후 DB 초기 세팅용).")
    @GetMapping("/center/all")
    public ApiResponse<List<CenterInfoResponse>> testAllCenterInfo() {
        List<CenterInfoResponse> response = mobilityTestService.getAllCenterInfo();
        return ApiResponse.success(response, "전국 센터 현황 목록 조회에 성공하였습니다.");
    }
}