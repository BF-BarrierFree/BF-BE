package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.course.domain.CourseCompanion;
import com.barrierfree.bf.course.domain.CourseDuration;
import com.barrierfree.bf.course.domain.CourseRegion;
import com.barrierfree.bf.course.domain.CourseTheme;
import com.barrierfree.bf.global.enums.MobilityType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 프론트엔드에서 5단계 UI 정보를 담아 요청하는 DTO
 */
public record AiCourseGenerateRequest(
    @NotNull(message = "지역을 선택해주세요.")
    CourseRegion region,

    @NotNull(message = "동행자를 선택해주세요.")
    CourseCompanion companion,

    // UI 3단계: 기존에 정의된 MobilityType을 그대로 재활용하여 필터링 로직 통일
    // (WHEELCHAIR, VISUAL_IMPAIRMENT 등) '없음'일 경우 빈 리스트 [] 전달
    List<MobilityType> mobilityTypes,

    @NotNull(message = "여행 테마를 선택해주세요.")
    CourseTheme theme,

    @NotNull(message = "여행 일정을 선택해주세요.")
    CourseDuration duration
) {}
