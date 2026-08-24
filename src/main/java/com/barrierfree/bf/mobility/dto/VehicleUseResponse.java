package com.barrierfree.bf.mobility.dto;

import com.barrierfree.bf.mobility.dto.external.VehicleUseRawResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleUseResponse {
    private String baseDateTime;
    private String centerName;
    private String totalVehicleCount;
    private String operatingVehicleCount;
    private String availableVehicleCount;
    private String waitingCount;

    public static VehicleUseResponse from(VehicleUseRawResponse.Item rawItem) {
        return VehicleUseResponse.builder()
            .baseDateTime(rawItem.getTotDt())
            .centerName(rawItem.getCntrNm())
            .totalVehicleCount(rawItem.getTvhclCntom())
            .operatingVehicleCount(rawItem.getOprVhclCntom())
            .availableVehicleCount(rawItem.getAvlVhclCntom())
            .waitingCount(rawItem.getWtngNocs())
            .build();
    }
}