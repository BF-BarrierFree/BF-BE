package com.barrierfree.bf.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * UI 2단계: 누구와 함께 여행하시나요?
 * (추후 LLM을 연동해 코스 제목을 짓거나 추천 가중치를 줄 때 활용합니다.)
 */
@Getter
@RequiredArgsConstructor
public enum CourseCompanion {
    ALONE("혼자 여행"),
    FRIENDS("친구와 여행"),
    COUPLE("연인과 여행"),
    FAMILY("가족과 여행");

    private final String label;
}
