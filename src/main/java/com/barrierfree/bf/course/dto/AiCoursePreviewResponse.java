package com.barrierfree.bf.course.dto;

import java.util.List;
import lombok.Builder;

/**
 * AI가 생성한 코스 조합 결과를 프론트엔드에 미리보기로 전달하기 위한 응답 DTO
 * 이 데이터는 아직 DB에 저장되지 않은 상태입니다.
 */
@Builder
public record AiCoursePreviewResponse(
    String courseTitle,           // 임시로 생성된 코스 제목 (예: "제주도 가족과 함께하는 힐링 여행")
    int totalDays,                // 총 여행 일수 (예: 1박 2일이면 2)
    List<AiCoursePlacePreview> places // 추천된 장소 리스트 (순서 보장)
) {
    public static AiCoursePreviewResponse of(String courseTitle, int totalDays, List<AiCoursePlacePreview> places) {
        return AiCoursePreviewResponse.builder()
            .courseTitle(courseTitle)
            .totalDays(totalDays)
            .places(places)
            .build();
    }
}
