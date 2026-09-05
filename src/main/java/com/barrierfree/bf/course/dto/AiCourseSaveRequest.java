package com.barrierfree.bf.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * AI 생성 코스 미리보기 확인 후, 프론트엔드에서 최종 저장을 요청할 때 사용하는 DTO
 */
public record AiCourseSaveRequest(
    @NotBlank(message = "코스 이름을 입력해주세요.")
    @Size(max = 15, message = "코스 이름은 15자 이내로 입력해주세요.")
    String title,

    @NotEmpty(message = "코스에는 최소 1개 이상의 장소가 필요합니다.")
    @Size(max = 7, message = "코스에는 최대 7개의 장소만 포함할 수 있습니다.")
    List<@NotNull(message = "장소 정보는 필수입니다.") @Valid AiCoursePlaceSaveDto> places
) {}
