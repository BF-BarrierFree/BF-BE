package com.barrierfree.bf.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자가 필요로 하는 접근성 시설 유형을 정의하는 Enum
 * 온보딩 시 다중 선택이 가능합니다.
 */
@Getter
@RequiredArgsConstructor
public enum FacilityType {

    ELEVATOR("엘리베이터"),
    ELECTRIC_WHEELCHAIR_RENTAL("전동 휠체어 대여"),
    BRAILLE_BLOCK("점자 블록"),
    SUBTITLE_SERVICE("자막 서비스"),
    ACCESSIBLE_RESTROOM("장애인 화장실"),
    WHEELCHAIR_SEAT("휠체어 좌석"),
    RAMP("경사로"),
    VOICE_GUIDANCE("음성 안내"),
    ACCESSIBLE_PARKING("장애인 주차"),
    REST_AREA("휴게 공간"),
    SIGN_LANGUAGE_INTERPRETATION("수어 통역"),
    NURSING_ROOM("수유실"),
    NONE("없음");

    private final String description;
}
