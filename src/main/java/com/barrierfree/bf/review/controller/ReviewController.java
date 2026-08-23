package com.barrierfree.bf.review.controller;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.review.dto.FacilityCountDto;
import com.barrierfree.bf.review.dto.ReviewCreateRequest;
import com.barrierfree.bf.review.dto.ReviewResponse;
import com.barrierfree.bf.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Review", description = "장소 리뷰 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "특정 장소에 리뷰 작성", description = "장소에 새로운 리뷰와 사진을 등록합니다. (로그인 필수)")
  @PostMapping(value = "/places/{placeId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<?> createReview(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "구글 맵스 장소 ID", example = "ChIJDY1UG9yZfDURPxpYsLCIEWg") @PathVariable
          String placeId,
      @Parameter(
              description = "리뷰 요청 데이터 (JSON 형식)",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
          @RequestPart("request")
          ReviewCreateRequest request,
      @Parameter(description = "리뷰 첨부 이미지 파일 리스트 (선택)")
          @RequestPart(value = "images", required = false)
          List<MultipartFile> images) {

    // 시큐리티를 통과했음에도 토큰이 누락되어 userId가 없다면 500 에러 대신 404 에러로 예쁘게 방어합니다.
    if (userId == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    reviewService.createReview(userId, placeId, request, images);
    return ApiResponse.successWithNoContent();
  }

  @Operation(
      summary = "전체 리뷰 조회 (글로벌 필터링 및 페이징)",
      description = "전체 지도에서 리뷰를 필터링하고 페이징하여 조회합니다. (비로그인 허용)")
  @Parameters({
    @Parameter(
        name = "sort",
        description = "정렬 기준",
        in = ParameterIn.QUERY,
        schema =
            @Schema(
                type = "string",
                allowableValues = {"createdAt,desc", "rating,desc", "rating,asc"}))
  })
  @GetMapping("/reviews")
  public ApiResponse<Page<ReviewResponse>> getAllReviews(
      @Parameter(description = "장소 카테고리 필터", example = "FOOD_CAFE")
          @RequestParam(value = "category", required = false)
          String category,
      @Parameter(
              description = "이동 유형 필터 (다중 선택 가능)",
              array =
                  @ArraySchema(
                      schema =
                          @Schema(
                              allowableValues = {
                                "WHEELCHAIR",
                                "STROLLER",
                                "COGNITIVE_DEVELOPMENTAL",
                                "VISUAL_IMPAIRMENT",
                                "HEARING_IMPAIRMENT",
                                "OTHER"
                              })))
          @RequestParam(value = "mobilities", required = false)
          List<String> mobilities,
      @Parameter(
              description = "접근성 시설 필터 (다중 선택 가능)",
              array =
                  @ArraySchema(
                      schema =
                          @Schema(
                              allowableValues = {
                                "ELEVATOR",
                                "ELECTRIC_WHEELCHAIR_RENTAL",
                                "BRAILLE_BLOCK",
                                "SUBTITLE_SERVICE",
                                "ACCESSIBLE_RESTROOM",
                                "WHEELCHAIR_SEAT",
                                "RAMP",
                                "VOICE_GUIDANCE",
                                "ACCESSIBLE_PARKING",
                                "REST_AREA",
                                "SIGN_LANGUAGE_INTERPRETATION",
                                "NURSING_ROOM",
                                "NONE"
                              })))
          @RequestParam(value = "facilities", required = false)
          List<String> facilities,
      @Parameter(
              description = "최소 별점 (1~5)",
              schema = @Schema(allowableValues = {"1", "2", "3", "4", "5"}),
              example = "1")
          @RequestParam(value = "minRating", required = false, defaultValue = "1")
          Integer minRating,
      @Parameter(hidden = true)
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    Page<ReviewResponse> response =
        reviewService.getAllReviews(category, minRating, mobilities, facilities, pageable);
    return ApiResponse.success(response);
  }

  @Operation(summary = "특정 장소의 리뷰 조회 (페이징)", description = "해당 장소의 리뷰 리스트를 페이징하여 조회합니다. (비로그인 허용)")
  @Parameters({
    @Parameter(
        name = "sort",
        description = "정렬 기준",
        in = ParameterIn.QUERY,
        schema =
            @Schema(
                type = "string",
                allowableValues = {"createdAt,desc", "rating,desc", "rating,asc"}))
  })
  @GetMapping("/places/{placeId}/reviews")
  public ApiResponse<Page<ReviewResponse>> getPlaceReviews(
      @Parameter(description = "구글 맵스 장소 ID", example = "ChIJDY1UG9yZfDURPxpYsLCIEWg") @PathVariable
          String placeId,
      @Parameter(
              description = "이동 유형 필터 (다중 선택 가능)",
              array =
                  @ArraySchema(
                      schema =
                          @Schema(
                              allowableValues = {
                                "WHEELCHAIR",
                                "STROLLER",
                                "COGNITIVE_DEVELOPMENTAL",
                                "VISUAL_IMPAIRMENT",
                                "HEARING_IMPAIRMENT",
                                "OTHER"
                              })))
          @RequestParam(value = "mobilities", required = false)
          List<String> mobilities,
      @Parameter(
              description = "접근성 시설 필터 (다중 선택 가능)",
              array =
                  @ArraySchema(
                      schema =
                          @Schema(
                              allowableValues = {
                                "ELEVATOR",
                                "ELECTRIC_WHEELCHAIR_RENTAL",
                                "BRAILLE_BLOCK",
                                "SUBTITLE_SERVICE",
                                "ACCESSIBLE_RESTROOM",
                                "WHEELCHAIR_SEAT",
                                "RAMP",
                                "VOICE_GUIDANCE",
                                "ACCESSIBLE_PARKING",
                                "REST_AREA",
                                "SIGN_LANGUAGE_INTERPRETATION",
                                "NURSING_ROOM",
                                "NONE"
                              })))
          @RequestParam(value = "facilities", required = false)
          List<String> facilities,
      @Parameter(
              description = "최소 별점 (1~5)",
              schema = @Schema(allowableValues = {"1", "2", "3", "4", "5"}),
              example = "1")
          @RequestParam(value = "minRating", required = false, defaultValue = "1")
          Integer minRating,
      @Parameter(hidden = true)
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    Page<ReviewResponse> response =
        reviewService.getPlaceReviews(placeId, null, minRating, mobilities, facilities, pageable);
    return ApiResponse.success(response);
  }

  @Operation(summary = "특정 장소의 시설 통계 조회", description = "해당 장소에 체크된 접근성 시설들의 카운트를 조회합니다. (비로그인 허용)")
  @GetMapping("/places/{placeId}/reviews/facilities/count")
  public ApiResponse<List<FacilityCountDto>> getFacilityCounts(
      @Parameter(description = "구글 맵스 장소 ID", example = "ChIJDY1UG9yZfDURPxpYsLCIEWg") @PathVariable
          String placeId) {

    List<FacilityCountDto> response = reviewService.getFacilityCounts(placeId);
    return ApiResponse.success(response);
  }

  @Operation(
      summary = "자연어 기반 임베딩 리뷰 검색 (페이징)",
      description = "Gemini 임베딩을 활용하여 문맥 기반으로 유사한 리뷰를 검색합니다. (비로그인 허용)")
  @GetMapping("/reviews/search")
  public ApiResponse<Page<ReviewResponse>> searchSimilarReviews(
      @Parameter(description = "자연어 검색어", example = "휠체어 타기 좋은 조용한 카페", required = true)
          @RequestParam
          String query,
      @PageableDefault(size = 10) Pageable pageable) {

    Page<ReviewResponse> response = reviewService.searchSimilarReviews(query, pageable);
    return ApiResponse.success(response);
  }
}
