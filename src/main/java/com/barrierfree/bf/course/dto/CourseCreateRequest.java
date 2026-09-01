package com.barrierfree.bf.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CourseCreateRequest(
    @NotBlank(message = "코스 이름을 입력해주세요.")
    @Size(max = 15, message = "코스 이름은 15자 이내로 입력해주세요.")
    String title,

    @NotEmpty(message = "코스에는 최소 1개 이상의 장소가 필요합니다.")
    List<@NotNull Long> savedPlaceIds // 유저가 선택한 저장된 장소(SavedPlace)의 ID 목록
) {
}
