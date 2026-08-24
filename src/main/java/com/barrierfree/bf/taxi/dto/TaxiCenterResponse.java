package com.barrierfree.bf.taxi.dto;

import com.barrierfree.bf.taxi.entity.TaxiCenter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaxiCenterResponse {

  private Long id;
  private String centerId;
  private String centerName;
  private String localGovName;
  private String roadAddress;
  private String phoneNumber;
  private String reservationUrl;
  private String appName;
  private String weekdayReservationStartTime;
  private String weekdayReservationEndTime;
  private String weekdayOperationStartTime;
  private String weekdayOperationEndTime;
  private String weekendOperationYn;
  private String weekendOperationExplanation;
  private String withinRegionName;
  private String outsideRegionName;
  private String targetExplanation;
  private String dailyUsageLimit;
  private String advanceReservationExplanation;
  private String basicChargeExplanation;
  private String extraChargeExplanation;
  private String reservationNotice;
  private String formSchema; // FE에서 파싱해서 사용할 동적 폼 스키마 JSON 문자열

  // 출발지로부터의 거리 (단위: km)
  private double distanceKm;

  public static TaxiCenterResponse from(TaxiCenter center, double distanceKm) {
    return TaxiCenterResponse.builder()
        .id(center.getId())
        .centerId(center.getCenterId())
        .centerName(center.getCenterName())
        .localGovName(center.getLocalGovName())
        .roadAddress(center.getRoadAddress())
        .phoneNumber(center.getPhoneNumber())
        .reservationUrl(center.getReservationUrl())
        .appName(center.getAppName())
        .weekdayReservationStartTime(center.getWeekdayReservationStartTime())
        .weekdayReservationEndTime(center.getWeekdayReservationEndTime())
        .weekdayOperationStartTime(center.getWeekdayOperationStartTime())
        .weekdayOperationEndTime(center.getWeekdayOperationEndTime())
        .weekendOperationYn(center.getWeekendOperationYn())
        .weekendOperationExplanation(center.getWeekendOperationExplanation())
        .withinRegionName(center.getWithinRegionName())
        .outsideRegionName(center.getOutsideRegionName())
        .targetExplanation(center.getTargetExplanation())
        .dailyUsageLimit(center.getDailyUsageLimit())
        .advanceReservationExplanation(center.getAdvanceReservationExplanation())
        .basicChargeExplanation(center.getBasicChargeExplanation())
        .extraChargeExplanation(center.getExtraChargeExplanation())
        .reservationNotice(center.getReservationNotice())
        .formSchema(center.getFormSchema())
        .distanceKm(Math.round(distanceKm * 10.0) / 10.0) // 소수점 첫째 자리까지만 표시
        .build();
  }
}
