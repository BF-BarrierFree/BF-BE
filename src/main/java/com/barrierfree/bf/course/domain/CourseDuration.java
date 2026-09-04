package com.barrierfree.bf.course.domain;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * UI 5단계: 여행 일정은 어떻게 되나요?
 * 알고리즘에서 코스를 구성할 '장소의 개수와 순서(조합 룰)'를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseDuration {
    HALF_DAY("반나절", 1, List.of("명소/테마", "식당/카페")), // 2개 장소 추천
    FULL_DAY("하루", 1, List.of("명소/테마", "식당/카페", "명소/테마", "식당/카페")), // 4개 장소 추천
    ONE_NIGHT_TWO_DAYS("1박 2일", 2, List.of("명소/테마", "식당/카페", "명소/테마", "숙박", "명소/테마", "식당/카페")),
    TWO_NIGHTS_MORE("2박 3일 이상", 3, List.of("명소/테마", "식당/카페", "명소/테마", "숙박", "명소/테마", "식당/카페", "명소/테마"));

    private final String label;
    private final int dayCount; // 숙박 등을 고려한 총 일수
    private final List<String> compositionRule; // 장소 추천 순서 뼈대
}
