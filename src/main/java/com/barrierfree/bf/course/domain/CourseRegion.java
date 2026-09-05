package com.barrierfree.bf.course.domain;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * UI 1단계: 어느 지역을 여행하시나요?
 * Google Places API 검색 시 Location Bias를 주기 위한 중심 좌표를 함께 들고 있습니다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseRegion {
    SEOUL("서울", 37.5665, 126.9780),
    BUSAN("부산", 35.1796, 129.0756),
    JEJU("제주", 33.4996, 126.5312),
    GYEONGJU("경주", 35.8562, 129.2247),
    JEONJU("전주", 35.8242, 127.1480),
    INCHEON("인천", 37.4563, 126.7052),
    DAEJEON("대전", 36.3504, 127.3845),
    DAEGU("대구", 35.8714, 128.6014),
    SUWON("수원", 37.2636, 127.0286),
    GANGNEUNG("강릉", 37.7519, 128.8761);

    private final String label;
    private final Double centerLat;
    private final Double centerLng;

    public static CourseRegion from(String value) {
        return Arrays.stream(values())
            .filter(region -> region.name().equalsIgnoreCase(value) || region.label.equals(value))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }
}
