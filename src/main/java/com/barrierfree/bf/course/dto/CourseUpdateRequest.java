package com.barrierfree.bf.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CourseUpdateRequest(
    @NotBlank(message = "코스 이름을 입력해주세요.")
    @Size(max = 15, message = "코스 이름은 15자 이내로 입력해주세요.")
    String title,

    // 수정 시 장소 순서가 바뀌거나 추가/삭제될 수 있으므로 전체 ID 리스트를 다시 받습니다.
    @NotEmpty(message = "코스에는 최소 1개 이상의 장소가 필요합니다.")
    List<@NotNull Long> savedPlaceIds
) {
}
