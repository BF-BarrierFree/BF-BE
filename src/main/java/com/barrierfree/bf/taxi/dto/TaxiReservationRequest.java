package com.barrierfree.bf.taxi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프론트엔드에서 택시 관련 요청을 보낼 때 사용하는 DTO 모음 클래스입니다.
 */
public class TaxiReservationRequest {

    @Getter
    @NoArgsConstructor
    @Schema(description = "예상 요금 산출 요청 DTO")
    public static class FareEstimate {
        @Schema(description = "선택한 택시 센터 ID", example = "1")
        private Long centerId;
        
        @Schema(description = "예상 이동 거리 (km)", example = "15.5")
        private double distanceKm;
        
        @Schema(description = "출발지 주소 (관외 할증 판단용)", example = "서울특별시 종로구 세종대로 175")
        private String startAddr;
        
        @Schema(description = "도착지 주소 (관외 할증 판단용)", example = "경기도 성남시 분당구 판교역로 146")
        private String endAddr;
    }

    @Getter
    @NoArgsConstructor
    public static class CreateReservation {
        private Long centerId;
        private String startAddr;
        private String endAddr;
        private String estimatedFare;
        private String generatedMessage;
        private String userMetadataJson;
    }
}