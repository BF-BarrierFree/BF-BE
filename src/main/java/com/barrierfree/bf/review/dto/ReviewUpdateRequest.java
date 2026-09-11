package com.barrierfree.bf.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "리뷰 수정 요청")
public record ReviewUpdateRequest(
    @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Schema(description = "리뷰 내용 본문", example = "단차가 없어서 휠체어로 들어가기 좋았습니다.")
        String content,
    @Schema(description = "사용자의 이동 유형", example = "[\"WHEELCHAIR\"]") List<String> mobilities,
    @Schema(description = "장소에 있는 접근성 시설", example = "[\"RAMP\"]") List<String> facilities,
    @Schema(description = "유지할 기존 이미지 URL. 생략/null이면 모두 유지, []이면 모두 제거. 새 파일은 images 파트로 추가")
        List<@NotBlank String> retainedImageUrls,
    @Size(max = 100) @Schema(description = "장소 지역 보완/수정. 생략하면 기존 값 유지", example = "서울 용산구")
        String region) {}
