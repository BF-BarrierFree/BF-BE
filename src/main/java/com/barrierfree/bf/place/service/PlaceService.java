package com.barrierfree.bf.place.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.GoogleAutocompleteResponseDto;
import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import com.barrierfree.bf.place.dto.PlaceAutocompleteResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

  private static final String AUTOCOMPLETE_URL =
      "https://places.googleapis.com/v1/places:autocomplete";
  private static final String TEXT_SEARCH_URL =
      "https://places.googleapis.com/v1/places:searchText";
  private static final String AUTOCOMPLETE_FIELD_MASK =
      "suggestions.placePrediction.placeId,"
          + "suggestions.placePrediction.text.text,"
          + "suggestions.placePrediction.structuredFormat.mainText.text,"
          + "suggestions.placePrediction.structuredFormat.secondaryText.text";
  private static final String PLACE_FIELD_MASK =
      "places.id,"
          + "places.displayName,"
          + "places.formattedAddress,"
          + "places.location,"
          + "places.accessibilityOptions";

  @Value("${google.places.api-key}")
  private String googleApiKey;

  private final WebClient webClient;

  public PlaceAutocompleteResponse autocomplete(
      String keyword, String categoryValue, Double lat, Double lng, Integer radius) {
    validateKeyword(keyword);
    PlaceCategory category = PlaceCategory.from(categoryValue);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("input", keyword);
    requestBody.put("languageCode", "ko");
    requestBody.put("includedRegionCodes", List.of("kr"));

    if (category != PlaceCategory.ETC) {
      requestBody.put("includedPrimaryTypes", category.getGoogleTypes());
    }
    putLocationBias(requestBody, lat, lng, radius);

    GoogleAutocompleteResponseDto googleResponse =
        webClient
            .post()
            .uri(AUTOCOMPLETE_URL)
            .header("X-Goog-Api-Key", googleApiKey)
            .header("X-Goog-FieldMask", AUTOCOMPLETE_FIELD_MASK)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                response -> {
                  log.error("Google Places Autocomplete failed. status={}", response.statusCode());
                  return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
                })
            .bodyToMono(GoogleAutocompleteResponseDto.class)
            .block();

    List<PlaceAutocompleteResponse.Suggestion> suggestions = new ArrayList<>();
    if (googleResponse != null && googleResponse.getSuggestions() != null) {
      suggestions =
          googleResponse.getSuggestions().stream()
              .filter(suggestion -> suggestion.getPlacePrediction() != null)
              .map(suggestion -> toSuggestion(suggestion, category))
              .toList();
    }

    return new PlaceAutocompleteResponse(suggestions);
  }

  public PlaceSearchResponse search(
      String keyword, String categoryValue, Double lat, Double lng, Integer radius) {
    validateKeyword(keyword);
    PlaceCategory category = PlaceCategory.from(categoryValue);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("textQuery", keyword);
    requestBody.put("languageCode", "ko");
    requestBody.put("regionCode", "KR");

    String includedType = category.getPrimaryGoogleType();
    if (includedType != null) {
      requestBody.put("includedType", includedType);
    }
    putLocationBias(requestBody, lat, lng, radius);

    GooglePlaceResponseDto googleResponse =
        webClient
            .post()
            .uri(TEXT_SEARCH_URL)
            .header("X-Goog-Api-Key", googleApiKey)
            .header("X-Goog-FieldMask", PLACE_FIELD_MASK)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                response -> {
                  log.error("Google Places Text Search failed. status={}", response.statusCode());
                  return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
                })
            .bodyToMono(GooglePlaceResponseDto.class)
            .block();

    List<PlaceSearchResponse.PlaceSummary> places = new ArrayList<>();
    if (googleResponse != null && googleResponse.getPlaces() != null) {
      places =
          googleResponse.getPlaces().stream()
              .map(place -> toPlaceSummary(place, category))
              .toList();
    }

    return new PlaceSearchResponse(places);
  }

  private PlaceAutocompleteResponse.Suggestion toSuggestion(
      GoogleAutocompleteResponseDto.Suggestion suggestion, PlaceCategory category) {
    GoogleAutocompleteResponseDto.PlacePrediction prediction = suggestion.getPlacePrediction();
    String name =
        prediction.getStructuredFormat() != null
                && prediction.getStructuredFormat().getMainText() != null
            ? prediction.getStructuredFormat().getMainText().getText()
            : null;
    String description = prediction.getText() == null ? null : prediction.getText().getText();
    String secondaryText =
        prediction.getStructuredFormat() != null
                && prediction.getStructuredFormat().getSecondaryText() != null
            ? prediction.getStructuredFormat().getSecondaryText().getText()
            : null;

    return new PlaceAutocompleteResponse.Suggestion(
        prediction.getPlaceId(), name, description, secondaryText, category, category.getLabel());
  }

  private PlaceSearchResponse.PlaceSummary toPlaceSummary(
      GooglePlaceResponseDto.Place place, PlaceCategory category) {
    GooglePlaceResponseDto.Location location = place.getLocation();
    GooglePlaceResponseDto.AccessibilityOptions accessibility = place.getAccessibilityOptions();

    return new PlaceSearchResponse.PlaceSummary(
        place.getId(),
        place.getDisplayName() == null ? null : place.getDisplayName().getText(),
        place.getFormattedAddress(),
        location == null ? null : location.getLatitude(),
        location == null ? null : location.getLongitude(),
        category,
        category.getLabel(),
        accessibility == null ? null : accessibility.getWheelchairAccessibleEntrance(),
        accessibility == null ? null : accessibility.getWheelchairAccessibleParking(),
        accessibility == null ? null : accessibility.getWheelchairAccessibleRestroom(),
        accessibility == null ? null : accessibility.getWheelchairAccessibleSeating());
  }

  private void putLocationBias(
      Map<String, Object> requestBody, Double lat, Double lng, Integer radius) {
    if (lat == null || lng == null) {
      return;
    }

    Map<String, Object> center = new HashMap<>();
    center.put("latitude", lat);
    center.put("longitude", lng);

    Map<String, Object> circle = new HashMap<>();
    circle.put("center", center);
    circle.put("radius", radius == null ? 500.0 : radius.doubleValue());

    requestBody.put("locationBias", Map.of("circle", circle));
  }

  private void validateKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }
}
