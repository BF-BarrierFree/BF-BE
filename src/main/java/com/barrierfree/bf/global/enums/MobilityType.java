package com.barrierfree.bf.global.enums;

/**
 * 무장애 지도 서비스 사용자의 주 이동 유형을 정의하는 Enum
 * 온보딩/리뷰 작성 시 nullable 또는 1가지 이상 다중 선택.
 */
public enum MobilityType {
    
    WHEELCHAIR("휠체어 사용자"),
    STROLLER("유모차 동반"),
    SENIOR("고령자 동반"),
    DISABLED_GUARDIAN("장애 동반 보호자"),
    PHYSICAL_DISABILITY("지체 장애"),
    VISUAL_IMPAIRMENT("시각 장애"),
    HEARING_IMPAIRMENT("청각 장애"),
    NONE("해당 없음");

    private final String description;

    MobilityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}