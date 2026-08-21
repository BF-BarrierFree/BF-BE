package com.barrierfree.bf.taxi.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지자체별 교통약자이동지원센터 현황 정보를 캐싱하는 Entity
 */
@Entity
@Table(name = "taxi_centers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxiCenter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String centerId;

    @Column(nullable = false, length = 100)
    private String centerName;

    @Column(length = 100)
    private String localGovName;

    @Column(length = 255)
    private String roadAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 50)
    private String phoneNumber;

    @Column(length = 255)
    private String reservationUrl;

    @Column(length = 100)
    private String appName;

    @Column(length = 10)
    private String weekdayReservationStartTime;

    @Column(length = 10)
    private String weekdayReservationEndTime;

    @Column(length = 10)
    private String weekdayOperationStartTime;

    @Column(length = 10)
    private String weekdayOperationEndTime;

    @Column(length = 10)
    private String weekendOperationYn;

    @Column(columnDefinition = "TEXT")
    private String weekendOperationExplanation;

    @Column(length = 255)
    private String withinRegionName;

    @Column(length = 255)
    private String outsideRegionName;

    @Column(columnDefinition = "TEXT")
    private String targetExplanation;

    @Column(length = 50)
    private String dailyUsageLimit;

    @Column(columnDefinition = "TEXT")
    private String advanceReservationExplanation;

    @Column(columnDefinition = "TEXT")
    private String basicChargeExplanation;

    @Column(columnDefinition = "TEXT")
    private String extraChargeExplanation;

    @Column(columnDefinition = "TEXT")
    private String reservationNotice;
    
    @Column(length = 20)
    private String referenceDate;

    // --- 추가됨: 배치 작업 시 LLM이 생성해둔 동적 폼 스키마 (JSON 형태의 문자열) ---
    @Column(columnDefinition = "TEXT")
    private String formSchema;

    @Builder
    public TaxiCenter(String centerId, String centerName, String localGovName, String roadAddress, 
                      Double latitude, Double longitude, String phoneNumber, String reservationUrl, 
                      String appName, String weekdayReservationStartTime, String weekdayReservationEndTime, 
                      String weekdayOperationStartTime, String weekdayOperationEndTime, String weekendOperationYn, 
                      String weekendOperationExplanation, String withinRegionName, String outsideRegionName, 
                      String targetExplanation, String dailyUsageLimit, String advanceReservationExplanation, 
                      String basicChargeExplanation, String extraChargeExplanation, String reservationNotice, 
                      String referenceDate, String formSchema) {
        this.centerId = centerId;
        this.centerName = centerName;
        this.localGovName = localGovName;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.reservationUrl = reservationUrl;
        this.appName = appName;
        this.weekdayReservationStartTime = weekdayReservationStartTime;
        this.weekdayReservationEndTime = weekdayReservationEndTime;
        this.weekdayOperationStartTime = weekdayOperationStartTime;
        this.weekdayOperationEndTime = weekdayOperationEndTime;
        this.weekendOperationYn = weekendOperationYn;
        this.weekendOperationExplanation = weekendOperationExplanation;
        this.withinRegionName = withinRegionName;
        this.outsideRegionName = outsideRegionName;
        this.targetExplanation = targetExplanation;
        this.dailyUsageLimit = dailyUsageLimit;
        this.advanceReservationExplanation = advanceReservationExplanation;
        this.basicChargeExplanation = basicChargeExplanation;
        this.extraChargeExplanation = extraChargeExplanation;
        this.reservationNotice = reservationNotice;
        this.referenceDate = referenceDate;
        this.formSchema = formSchema;
    }

    public void updateInfo(TaxiCenter newData) {
        this.centerName = newData.getCenterName();
        this.localGovName = newData.getLocalGovName();
        this.roadAddress = newData.getRoadAddress();
        this.latitude = newData.getLatitude();
        this.longitude = newData.getLongitude();
        this.phoneNumber = newData.getPhoneNumber();
        this.reservationUrl = newData.getReservationUrl();
        this.appName = newData.getAppName();
        this.weekdayReservationStartTime = newData.getWeekdayReservationStartTime();
        this.weekdayReservationEndTime = newData.getWeekdayReservationEndTime();
        this.weekdayOperationStartTime = newData.getWeekdayOperationStartTime();
        this.weekdayOperationEndTime = newData.getWeekdayOperationEndTime();
        this.weekendOperationYn = newData.getWeekendOperationYn();
        this.weekendOperationExplanation = newData.getWeekendOperationExplanation();
        this.withinRegionName = newData.getWithinRegionName();
        this.outsideRegionName = newData.getOutsideRegionName();
        this.targetExplanation = newData.getTargetExplanation();
        this.dailyUsageLimit = newData.getDailyUsageLimit();
        this.advanceReservationExplanation = newData.getAdvanceReservationExplanation();
        this.basicChargeExplanation = newData.getBasicChargeExplanation();
        this.extraChargeExplanation = newData.getExtraChargeExplanation();
        this.reservationNotice = newData.getReservationNotice();
        this.referenceDate = newData.getReferenceDate();
        
        // 규정이 갱신되었을 수 있으므로 LLM이 새로 생성한 스키마로 업데이트
        if (newData.getFormSchema() != null) {
            this.formSchema = newData.getFormSchema();
        }
    }
}