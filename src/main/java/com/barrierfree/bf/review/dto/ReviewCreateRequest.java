package com.barrierfree.bf.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // MultipartFile과 JSON 폼 데이터 바인딩을 위해 필요
@NoArgsConstructor
@Schema(description = "리뷰 생성 요청 DTO (multipart/form-data의 request 파트)")
public class ReviewCreateRequest {

    @NotBlank(message = "장소 ID는 필수입니다.")
    @Schema(description = "구글 맵스 장소 ID", example = "ChIJDY1UG9yZfDURPxpYsLCIEWg")
    private String placeId;

    @NotBlank(message = "장소명은 필수입니다.")
    @Schema(description = "장소 이름 (임베딩 및 글로벌 목록 표시용)", example = "스시지현")
    private String placeName;

    @NotBlank(message = "장소 카테고리는 필수입니다.")
    @Schema(description = "장소 카테고리 (FOOD_CAFE, TOUR_CULTURE 등)", example = "FOOD_CAFE")
    private String category;

    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하이어야 합니다.")
    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Schema(description = "리뷰 내용 본문", example = "단차가 없어서 휠체어로 들어가기 좋았습니다.")
    private String content;

    @Schema(description = "사용자의 이동 유형 (선택하지 않을 시 빈 배열 전송)", example = "[\"WHEELCHAIR\", \"STROLLER\"]")
    private List<String> mobilities;

    @Schema(description = "장소에 있는 접근성 시설 (선택하지 않을 시 빈 배열 전송)", example = "[\"ELEVATOR\", \"RAMP\"]")
    private List<String> facilities;
}