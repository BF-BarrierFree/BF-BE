package com.barrierfree.bf.course.dto;

import com.barrierfree.bf.course.entity.Course;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record CourseResponse(
    Long id,
    String title,
    boolean isAiGenerated,
    LocalDateTime createdAt,
    List<CoursePlaceResponse> places
) {
    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
            .id(course.getId())
            .title(course.getTitle())
            .isAiGenerated(course.isAiGenerated())
            .createdAt(course.getCreatedAt())
            .places(course.getPlaces().stream()
                .map(CoursePlaceResponse::from)
                .collect(Collectors.toList()))
            .build();
    }
}
