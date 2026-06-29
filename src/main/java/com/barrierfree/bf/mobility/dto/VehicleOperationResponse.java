package com.barrierfree.bf.mobility.dto;

import com.barrierfree.bf.mobility.dto.external.VehicleOperationRawResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleOperationResponse {

    private String centerName;
    private String vehicleId;
    private String vehicleModelName;
    private String vehicleUsageName;
    private boolean isOperationAvailable;
    private String baseDateTime;

    /**
     * 외부 API의 Raw Item 데이터를 우리 서비스 규격의 응답 DTO로 변환합니다.
     */
    public static VehicleOperationResponse from(VehicleOperationRawResponse.Item rawItem) {
        return VehicleOperationResponse.builder()
            .centerName(rawItem.getCntrNm())
            .vehicleId(rawItem.getVhclId())
            .vehicleModelName(rawItem.getVhclMdlNm())
            .vehicleUsageName(rawItem.getVhclUsgNm())
            .isOperationAvailable("Y".equalsIgnoreCase(rawItem.getOperSttsYn()))
            .baseDateTime(rawItem.getTotDt())
            .build();
    }
}