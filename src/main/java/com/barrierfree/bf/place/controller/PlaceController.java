package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.dto.PlaceAutocompleteResponse;
import com.barrierfree.bf.place.dto.PlaceSearchHistoryResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceSearchHistoryService;
import com.barrierfree.bf.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
  @Operation(
      summary = "장소 자동완성",
      description =
          "사용자가 입력 중인 키워드로 장소 후보를 조회합니다. 예: keyword=롯데 입력 시 롯데월드 등 관련 장소 후보를 반환합니다.")
  public ApiResponse<PlaceAutocompleteResponse> autocomplete(
      @Parameter(description = "검색 키워드. 예: 롯데", example = "롯데")
          @RequestParam(required = false)
          String keyword,
      @Parameter(description = "검색 키워드 alias. keyword 대신 사용할 수 있습니다.", example = "롯데")
          @RequestParam(required = false)
          String input,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "검색 위치 bias 위도", example = "37.511")
          @RequestParam(required = false)
          Double lat,
      @Parameter(description = "검색 위치 bias 경도", example = "127.098")
          @RequestParam(required = false)
          Double lng,
      @Parameter(description = "검색 위치 bias 반경 meter", example = "1000")
          @RequestParam(defaultValue = "500", required = false)
          Integer radius) {
    String searchKeyword = keyword != null ? keyword : input;
    PlaceAutocompleteResponse response =
        placeService.autocomplete(searchKeyword, category, lat, lng, radius);
    return ApiResponse.success(response, "장소 자동완성 조회에 성공했습니다.");
  }

  @GetMapping("/search")
  @Operation(
      summary = "키워드 장소 검색",
      description =
          "키워드와 선택 카테고리로 장소 목록을 검색합니다. 첫 페이지 검색 시 검색 기록을 저장하고, 무한스크롤은 nextPageToken을 pageToken으로 전달해 조회합니다.")
  public ApiResponse<PlaceSearchResponse> search(
      @Parameter(description = "검색 키워드. 예: 롯데월드", example = "롯데월드")
          @RequestParam(required = false)
          String keyword,
      @Parameter(description = "검색 키워드 alias. keyword 대신 사용할 수 있습니다.", example = "롯데월드")
          @RequestParam(required = false)
          String query,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "검색 위치 bias 위도", example = "37.511")
          @RequestParam(required = false)
          Double lat,
      @Parameter(description = "검색 위치 bias 경도", example = "127.098")
          @RequestParam(required = false)
          Double lng,
      @Parameter(description = "검색 위치 bias 반경 meter", example = "1000")
          @RequestParam(defaultValue = "500", required = false)
          Integer radius,
      @Parameter(description = "한 페이지 결과 개수. 최대 20개입니다.", example = "10")
          @RequestParam(defaultValue = "20", required = false)
          Integer pageSize,
      @Parameter(description = "이전 검색 응답의 nextPageToken. 무한스크롤 다음 페이지 조회에 사용합니다.")
          @RequestParam(required = false)
          String pageToken) {
    String searchKeyword = keyword != null ? keyword : query;
    PlaceSearchResponse response =
        placeService.search(searchKeyword, category, lat, lng, radius, pageSize, pageToken);
    return ApiResponse.success(response, "장소 검색에 성공했습니다.");
  }

  @GetMapping("/search-histories")
  @Operation(summary = "최근 장소 검색 기록 조회", description = "최근 검색한 장소 키워드 목록을 조회합니다.")
  public ApiResponse<PlaceSearchHistoryResponse> getSearchHistories(
      @Parameter(description = "조회할 검색 기록 개수. 최대 50개입니다.", example = "10")
          @RequestParam(defaultValue = "10", required = false)
          Integer size) {
    PlaceSearchHistoryResponse response = placeSearchHistoryService.getRecent(size);
    return ApiResponse.success(response, "최근 장소 검색 기록 조회에 성공했습니다.");
  }
}
