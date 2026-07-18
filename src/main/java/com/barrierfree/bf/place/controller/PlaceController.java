package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.dto.PlaceAutocompleteResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
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

  @GetMapping("/autocomplete")
  @Operation(
      summary = "장소 자동완성",
      description = "사용자가 입력 중인 키워드로 장소 후보를 조회합니다. 예: keyword=롯데 입력 시 롯데월드 등 관련 장소 후보를 반환합니다.")
  public ApiResponse<PlaceAutocompleteResponse> autocomplete(
      @Parameter(description = "검색 키워드. 예: 롯데", example = "롯데") @RequestParam(required = false)
          String keyword,
      @Parameter(description = "검색 키워드 alias. keyword 대신 사용할 수 있습니다.", example = "롯데")
          @RequestParam(required = false)
          String input,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC 또는 한국어 라벨 사용 가능",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "검색 위치 bias 위도", example = "37.511") @RequestParam(required = false)
          Double lat,
      @Parameter(description = "검색 위치 bias 경도", example = "127.098") @RequestParam(required = false)
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
      summary = "장소 검색",
      description = "키워드와 선택 카테고리로 장소 목록을 검색합니다. 자동완성 후보 선택 후 상세 목록을 보여줄 때 사용할 수 있습니다.")
  public ApiResponse<PlaceSearchResponse> search(
      @Parameter(description = "검색 키워드. 예: 롯데월드", example = "롯데월드") @RequestParam(required = false)
          String keyword,
      @Parameter(description = "검색 키워드 alias. keyword 대신 사용할 수 있습니다.", example = "롯데월드")
          @RequestParam(required = false)
          String query,
      @Parameter(
              description =
                  "장소 카테고리. FOOD_CAFE, TOUR_CULTURE, PARK_TRAIL, LODGING, TRANSPORTATION, PUBLIC_FACILITY, ETC 또는 한국어 라벨 사용 가능",
              example = "TOUR_CULTURE")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "검색 위치 bias 위도", example = "37.511") @RequestParam(required = false)
          Double lat,
      @Parameter(description = "검색 위치 bias 경도", example = "127.098") @RequestParam(required = false)
          Double lng,
      @Parameter(description = "검색 위치 bias 반경 meter", example = "1000")
          @RequestParam(defaultValue = "500", required = false)
          Integer radius) {
    String searchKeyword = keyword != null ? keyword : query;
    PlaceSearchResponse response = placeService.search(searchKeyword, category, lat, lng, radius);
    return ApiResponse.success(response, "장소 검색에 성공했습니다.");
  }
}
