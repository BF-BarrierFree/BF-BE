package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.dto.GoogleAutocompleteResponseDto;
import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import com.barrierfree.bf.place.service.PlaceTestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test/places")
public class PlaceTestController {

  private final PlaceTestService placeTestService;

  /**
   * 1. [추가] 구글 Places API 자동완성(Autocomplete) 테스트 엔드포인트 사용 예시: GET
   * /api/v1/test/places/autocomplete?input=홍대 카
   */
  @GetMapping("/autocomplete")
  public ApiResponse<GoogleAutocompleteResponseDto> autocompletePlaces(@RequestParam String input) {

    GoogleAutocompleteResponseDto response = placeTestService.autocomplete(input);
    return ApiResponse.success(response, "구글 장소 자동완성 테스트에 성공했습니다.");
  }

  /**
   * 2. 구글 Places API 텍스트 검색 (위치 기반 옵션 포함) 테스트 엔드포인트 사용 예시: GET
   * /api/v1/test/places/search/text?query=홍대 카페&lat=37.5559&lng=126.9242&radius=500
   */
  @GetMapping("/search/text")
  public ApiResponse<GooglePlaceResponseDto> searchPlacesByText(
      @RequestParam String query,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng,
      @RequestParam(defaultValue = "500", required = false) Integer radius) {

    GooglePlaceResponseDto response = placeTestService.searchByText(query, lat, lng, radius);
    return ApiResponse.success(response, "구글 텍스트 및 위치 기반 장소 검색 테스트에 성공했습니다.");
  }

  /**
   * 3. [수정] 구글 Places API 위치(좌표) 기반 검색 + 카테고리 필터링(includedTypes) 사용 예시: GET
   * /api/v1/test/places/search/nearby?lat=37.5559&lng=126.9242&radius=500&types=restaurant,cafe
   */
  @GetMapping("/search/nearby")
  public ApiResponse<GooglePlaceResponseDto> searchPlacesNearby(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(defaultValue = "500") int radius,
      @RequestParam(required = false) List<String> types) { // 타입 필터링 옵션 추가

    GooglePlaceResponseDto response = placeTestService.searchNearby(lat, lng, radius, types);
    return ApiResponse.success(response, "구글 위치 및 카테고리 기반 장소 검색 테스트에 성공했습니다.");
  }

  /** 4. 구글 Places API 특정 장소 상세 조회 (모든 필드 포함) 사용 예시: GET /api/v1/test/places/details/{placeId} */
  @GetMapping("/details/{placeId}")
  public ApiResponse<GooglePlaceResponseDto.Place> getPlaceDetails(@PathVariable String placeId) {

    GooglePlaceResponseDto.Place response = placeTestService.getPlaceDetails(placeId);
    return ApiResponse.success(response, "구글 장소 상세 조회 테스트에 성공했습니다.");
  }
}
