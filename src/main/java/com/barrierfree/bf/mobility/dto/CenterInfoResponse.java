package com.barrierfree.bf.mobility.dto;

import com.barrierfree.bf.mobility.dto.external.CenterInfoRawResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CenterInfoResponse {
    private String centerId;
    private String centerName;
    private String address;
    private String lat;
    private String lng;
    private String phone;
    private String reserveUrl;
    private String operationStartTime;
    private String operationEndTime;
    private String outsideOperRegion;
    private String targetExpln;
    private String basicCharge;
    private String extraCharge;

    public static CenterInfoResponse from(CenterInfoRawResponse.Item rawItem) {
        return CenterInfoResponse.builder()
            .centerId(rawItem.getCntrId())
            .centerName(rawItem.getCntrNm())
            .address(rawItem.getCntrRoadNmAddr())
            .lat(rawItem.getLat())
            .lng(rawItem.getLot())
            .phone(rawItem.getCntrTelno())
            .reserveUrl(rawItem.getRsvtSiteUrlAddr())
            .operationStartTime(rawItem.getWkdyOprBgngTm())
            .operationEndTime(rawItem.getWkdyOprEndTm())
            .outsideOperRegion(rawItem.getBtjrOprRgnNm())
            .targetExpln(rawItem.getUtztnTrgtExpln())
            .basicCharge(rawItem.getBscCrgExpln())
            .extraCharge(rawItem.getExchrgCrgExpln())
            .build();
    }
}