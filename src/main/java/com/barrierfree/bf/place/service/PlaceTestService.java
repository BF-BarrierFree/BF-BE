package com.barrierfree.bf.place.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.dto.GoogleAutocompleteResponseDto;
import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceTestService {

  @Value("${google.places.api-key}")
  private String googleApiKey;

  private final WebClient webClient;

  private static final String BASE_FIELDS =
      "id,displayName,formattedAddress,location,reviews,editorialSummary,generativeSummary";
  private static final String ACCESSIBILITY_FIELDS = ",accessibilityOptions";
  // 3. 부가 정보 필드 (전화번호, 웹사이트, 반려견, 주차, 영업시간, 사진)
  private static final String EXTRA_INFO_FIELDS =
      ",nationalPhoneNumber,websiteUri,allowsDogs,parkingOptions,regularOpeningHours,photos";

  private static final String PLACE_FIELD_MASK =
      "places."
          + BASE_FIELDS.replace(",", ",places.")
          + ACCESSIBILITY_FIELDS.replace(",", ",places.")
          + EXTRA_INFO_FIELDS.replace(",", ",places.");

  private static final String PLACE_DETAIL_FIELD_MASK =
      BASE_FIELDS + ACCESSIBILITY_FIELDS + EXTRA_INFO_FIELDS;

  /** [신규 추가] 0. 장소 자동완성 (Autocomplete) */
  public GoogleAutocompleteResponseDto autocomplete(String input) {
    String url = "https://places.googleapis.com/v1/places:autocomplete";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("input", input);
    requestBody.put("languageCode", "ko");

    log.info("구글 장소 자동완성 API 호출 - 입력값: {}", input);

    return webClient
        .post()
        .uri(url)
        .header("X-Goog-Api-Key", googleApiKey)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            clientResponse -> {
              log.error("구글 Places API Autocomplete 에러 발생: 상태코드 {}", clientResponse.statusCode());
              return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
            })
        .bodyToMono(GoogleAutocompleteResponseDto.class)
        .block();
  }

  /** 1. 텍스트(검색어) 기반 장소 검색 + 위치 편향(locationBias) 옵션 */
  public GooglePlaceResponseDto searchByText(
      String textQuery, Double lat, Double lng, Integer radius) {
    String url = "https://places.googleapis.com/v1/places:searchText";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("textQuery", textQuery);
    requestBody.put("languageCode", "ko");

    if (lat != null && lng != null && radius != null) {
      Map<String, Object> locationBias = new HashMap<>();
      Map<String, Object> circle = new HashMap<>();
      Map<String, Object> center = new HashMap<>();

      center.put("latitude", lat);
      center.put("longitude", lng);
      circle.put("center", center);
      circle.put("radius", radius.doubleValue());
      locationBias.put("circle", circle);
      requestBody.put("locationBias", locationBias);
    }

    log.info("구글 텍스트 검색 API 호출 - 검색어: {}, 위도: {}, 경도: {}", textQuery, lat, lng);

    return webClient
        .post()
        .uri(url)
        .header("X-Goog-Api-Key", googleApiKey)
        .header("X-Goog-FieldMask", PLACE_FIELD_MASK)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            clientResponse -> {
              log.error("구글 Places API TextSearch 에러 발생: 상태코드 {}", clientResponse.statusCode());
              return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
            })
        .bodyToMono(GooglePlaceResponseDto.class)
        .block();
  }

  /** 2. [수정됨] 위치 기반(좌표 + 반경) 장소 검색 + 카테고리 필터링(includedTypes) */
  public GooglePlaceResponseDto searchNearby(
      double latitude, double longitude, int radius, List<String> types) {
    String url = "https://places.googleapis.com/v1/places:searchNearby";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("languageCode", "ko");

    if (types != null && !types.isEmpty()) {
      requestBody.put("includedTypes", types);
    }

    Map<String, Object> locationRestriction = new HashMap<>();
    Map<String, Object> circle = new HashMap<>();
    Map<String, Object> center = new HashMap<>();

    center.put("latitude", latitude);
    center.put("longitude", longitude);
    circle.put("center", center);
    circle.put("radius", (double) radius);
    locationRestriction.put("circle", circle);
    requestBody.put("locationRestriction", locationRestriction);

    log.info(
        "구글 위치 기반 검색 API 호출 - 위도: {}, 경도: {}, 반경: {}m, 필터타입: {}",
        latitude,
        longitude,
        radius,
        types);

    return webClient
        .post()
        .uri(url)
        .header("X-Goog-Api-Key", googleApiKey)
        .header("X-Goog-FieldMask", PLACE_FIELD_MASK)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            clientResponse -> {
              log.error("구글 Places API NearbySearch 에러 발생: 상태코드 {}", clientResponse.statusCode());
              return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
            })
        .bodyToMono(GooglePlaceResponseDto.class)
        .block();
  }

  /** 3. 특정 장소 상세 조회 */
  public GooglePlaceResponseDto.Place getPlaceDetails(String placeId) {
    String url =
        UriComponentsBuilder.fromHttpUrl("https://places.googleapis.com/v1/places/{placeId}")
            .queryParam("languageCode", "ko")
            .buildAndExpand(placeId)
            .toUriString();

    log.info("구글 장소 상세 조회 - placeId: {}", placeId);

    GooglePlaceResponseDto.Place place =
        webClient
            .get()
            .uri(url)
            .header("X-Goog-Api-Key", googleApiKey)
            .header("X-Goog-FieldMask", PLACE_DETAIL_FIELD_MASK)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                clientResponse -> {
                  log.error("구글 Places API Details 에러 발생: 상태코드 {}", clientResponse.statusCode());
                  return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
                })
            .bodyToMono(GooglePlaceResponseDto.Place.class)
            .block();

    if (place == null) {
      throw new CustomException(ErrorCode.FACILITY_NOT_FOUND);
    }

    return place;
  }
}
