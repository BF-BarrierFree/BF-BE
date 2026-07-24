package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.domain.AccessibilityFacility;
import com.barrierfree.bf.place.domain.AccessibilityUserType;
import com.barrierfree.bf.place.dto.PlaceAutocompleteResponse;
import com.barrierfree.bf.place.dto.PlaceSearchHistoryResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceSearchHistoryService;
import com.barrierfree.bf.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
@Tag(name = "Place", description = "장소 검색 및 자동완성 API")
public class PlaceController {

  private final PlaceService placeService;
  private final PlaceSearchHistoryService placeSearchHistoryService;

  @GetMapping("/autocomplete")
  @Operation(summary = "장소 자동완성", description = "사용자가 입력 중인 텍스트로 장소 후보를 조회합니다.")
  public ApiResponse<PlaceAutocompleteResponse> autocomplete(
      @Parameter(description = "검색어", example = "롯데")
          @RequestParam(required = false)
          String keyword,
      @Parameter(description = "keyword의 alias", example = "롯데")
          @RequestParam(required = false)
          String input,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "위도", example = "37.511")
          @RequestParam(required = false)
          Double lat,
      @Parameter(description = "경도", example = "127.098")
          @RequestParam(required = false)
          Double lng,
      @Parameter(description = "bias 반경(m)", example = "500")
          @RequestParam(defaultValue = "500", required = false)
          Integer radius) {
    String searchKeyword = keyword != null ? keyword : input;
    PlaceAutocompleteResponse response =
        placeService.autocomplete(searchKeyword, category, lat, lng, radius);
    return ApiResponse.success(response, "장소 자동완성 조회 성공");
  }

  @GetMapping("/search")
  @Operation(
      summary = "장소 검색",
      description = "키워드와 카테고리로 장소 목록을 검색합니다. userTypes/facilities는 콤마로 여러 개 입력합니다.")
  public ApiResponse<PlaceSearchResponse> search(
      @Parameter(description = "검색어", example = "롯데")
          @RequestParam(required = false)
          String keyword,
      @Parameter(description = "keyword의 alias", example = "롯데")
          @RequestParam(required = false)
          String query,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "위도", example = "37.511")
          @RequestParam(required = false)
          Double lat,
      @Parameter(description = "경도", example = "127.098")
          @RequestParam(required = false)
          Double lng,
      @Parameter(description = "bias 반경(m)", example = "500")
          @RequestParam(defaultValue = "500", required = false)
          Integer radius,
      @Parameter(description = "한 번에 반환할 개수. 최대 20개.", example = "10")
          @RequestParam(defaultValue = "20", required = false)
          Integer pageSize,
      @Parameter(description = "이전 검색 응답의 nextPageToken")
          @RequestParam(required = false)
          String pageToken,
      @Parameter(
              description = "이용자 유형. 예: 휠체어 이용자,유아차 동반,인지/발달 장애,시각 장애,청각 장애",
              schema = @Schema(type = "string"))
          @RequestParam(required = false)
          String userTypes,
      @Parameter(
              description = "필요시설. 예: 엘리베이터,경사로,장애인 화장실,수어통역,장애인 주차장,자막 서비스,전동 휠체어 대여,수유실,휠체어 좌석,휴게공간,음성안내,점자블록",
              schema = @Schema(type = "string"))
          @RequestParam(required = false)
          String facilities) {
    String searchKeyword = keyword != null ? keyword : query;
    List<AccessibilityUserType> parsedUserTypes = parseUserTypes(userTypes);
    List<AccessibilityFacility> parsedFacilities = parseFacilities(facilities);
    PlaceSearchResponse response =
        placeService.search(
            searchKeyword,
            category,
            lat,
            lng,
            radius,
            pageSize,
            pageToken,
            parsedUserTypes,
            parsedFacilities);
    return ApiResponse.success(response, "장소 검색에 성공했습니다.");
  }

  @GetMapping("/search-histories")
  @Operation(summary = "최근 장소 검색 기록 조회", description = "최근 검색한 장소 키워드 목록을 조회합니다.")
  public ApiResponse<PlaceSearchHistoryResponse> getSearchHistories(
      @Parameter(description = "조회할 검색 기록 수. 최대 50개.", example = "10")
          @RequestParam(defaultValue = "10", required = false)
          Integer size) {
    PlaceSearchHistoryResponse response = placeSearchHistoryService.getRecent(size);
    return ApiResponse.success(response, "최근 장소 검색 기록 조회 성공");
  }

  private List<AccessibilityUserType> parseUserTypes(String rawValues) {
    return parseDelimitedValues(rawValues).stream()
        .map(AccessibilityUserType::from)
        .filter(value -> value != null)
        .distinct()
        .toList();
  }

  private List<AccessibilityFacility> parseFacilities(String rawValues) {
    return parseDelimitedValues(rawValues).stream()
        .map(AccessibilityFacility::from)
        .filter(value -> value != null)
        .distinct()
        .toList();
  }

  private List<String> parseDelimitedValues(String rawValues) {
    if (rawValues == null || rawValues.isBlank()) {
      return List.of();
    }

    return Arrays.stream(rawValues.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
  }
}
