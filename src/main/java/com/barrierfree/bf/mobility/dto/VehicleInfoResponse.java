package com.barrierfree.bf.mobility.dto;

import com.barrierfree.bf.mobility.dto.external.VehicleInfoRawResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleInfoResponse {
    private String vehicleId;
    private String vehicleModelName;
    private String makeYear;
    private String wheelchairSpaceCount;
    private String totalSeatCount;

    public static VehicleInfoResponse from(VehicleInfoRawResponse.Item rawItem) {
        return VehicleInfoResponse.builder()
            .vehicleId(rawItem.getVhclId())
            .vehicleModelName(rawItem.getVhclMdlNm())
            .makeYear(rawItem.getFbctnYr())
            .wheelchairSpaceCount(rawItem.getWchrActcCntom())
            .totalSeatCount(rawItem.getRdcpctCnt())
            .build();
    }
}