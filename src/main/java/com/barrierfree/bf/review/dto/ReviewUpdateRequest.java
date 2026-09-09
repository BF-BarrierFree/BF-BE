package com.barrierfree.bf.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "리뷰 수정 요청")
public record ReviewUpdateRequest(
    @NotNull(message = "별점은 필수입니다.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하이어야 합니다.")
        @Schema(description = "별점 (1~5)", example = "5")
        Integer rating,
    @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Schema(description = "리뷰 내용 본문", example = "단차가 없어서 휠체어로 들어가기 좋았습니다.")
        String content,
    @Schema(description = "사용자의 이동 유형", example = "[\"WHEELCHAIR\"]") List<String> mobilities,
    @Schema(description = "장소에 있는 접근성 시설", example = "[\"RAMP\"]") List<String> facilities) {}
