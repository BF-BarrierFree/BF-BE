package com.barrierfree.bf.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자의 이동 유형을 정의하는 Enum
 * 온보딩 시 다중 선택이 가능합니다.
 */
@Getter
@RequiredArgsConstructor
public enum MobilityType {

    WHEELCHAIR("휠체어 사용"),
    VISUAL_IMPAIRMENT("시각 장애"),
    HEARING_IMPAIRMENT("청각 장애"),
    COGNITIVE_DEVELOPMENTAL("인지/발달 장애"),
    STROLLER("유아차 사용"),
    OTHER("기타");

    private final String description;
}
