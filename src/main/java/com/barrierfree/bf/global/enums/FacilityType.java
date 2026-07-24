package com.barrierfree.bf.global.enums;

/**
 * 무장애 시설 종류를 정의하는 Enum
 * 사용자의 선호 접근성 시설, 장소의 제공 시설, 리뷰의 태그 등에서 공통으로 사용됩니다.
 */
public enum FacilityType {
    
    WHEELCHAIR_ACCESSIBLE("휠체어 접근 가능"),
    ELEVATOR("엘리베이터"),
    DISABLED_RESTROOM("장애인 화장실"),
    DISABLED_PARKING("장애인 주차장"),
    BRAILLE_BLOCK("점자 블록"),
    AUDIO_GUIDE("음성 안내"),
    SIGN_LANGUAGE("수어 통역"),
    SUBTITLE_SERVICE("자막 서비스"),
    RAMP("경사로"),
    WIDE_AISLE("넓은 통로"),
    NURSING_ROOM("수유실"),
    NO_STEP("단차 없음"),
    ACCOMPANIMENT_SERVICE("동행 서비스"),
    REST_AREA("휴게 공간"),
    ELECTRIC_WHEELCHAIR_RENTAL("전동 휠체어 대여");

    private final String description;

    FacilityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}