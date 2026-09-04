package com.barrierfree.bf.course.domain;

import com.barrierfree.bf.place.domain.PlaceCategory;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * UI 4단계: 어떤 여행을 하고 싶으신가요?
 * 기존 PlaceCategory와 맵핑하여 구글 API 텍스트 검색 키워드로 활용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseTheme {
    FOOD_CAFE("맛집·카페", List.of(PlaceCategory.FOOD_CAFE)),
    NATURE_HEALING("자연·휴식", List.of(PlaceCategory.PARK_TRAIL)),
    CULTURE_ART("문화·예술", List.of(PlaceCategory.TOUR_CULTURE)),
    ATTRACTION("관광·명소", List.of(PlaceCategory.TOUR_CULTURE)),
    SHOPPING("쇼핑", List.of(PlaceCategory.ETC)); // 쇼핑은 ETC로 두고 검색 키워드에 '쇼핑', '몰' 등을 강제 추가

    private final String label;
    private final List<PlaceCategory> targetCategories;
}
