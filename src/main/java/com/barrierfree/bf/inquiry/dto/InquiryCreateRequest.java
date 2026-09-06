package com.barrierfree.bf.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
    @Schema(
            description = "문의 유형: service_usage, course_error, accessibility_info_error, content, etc",
            example = "service_usage")
        @NotBlank
        @Size(max = 40)
        String category,
    @Schema(description = "문의 제목", example = "코스 저장이 되지 않습니다")
        @NotBlank
        @Size(max = 100)
        String title,
    @Schema(description = "문의 내용", example = "코스를 저장하면 오류 메시지가 표시됩니다.")
        @NotBlank
        String content) {}
