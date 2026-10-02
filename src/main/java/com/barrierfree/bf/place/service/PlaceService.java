package com.barrierfree.bf.place.service;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.GoogleAutocompleteResponseDto;
import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import com.barrierfree.bf.place.dto.PlaceAutocompleteResponse;
import com.barrierfree.bf.place.dto.PlaceDetailResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.dto.PublicBarrierFreeInfo;
import com.barrierfree.bf.place.dto.PublicBarrierFreePlace;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

  private static final String AUTOCOMPLETE_URL =
      "https://places.googleapis.com/v1/places:autocomplete";
  private static final String TEXT_SEARCH_URL =
      "https://places.googleapis.com/v1/places:searchText";
  private static final String NEARBY_SEARCH_URL =
      "https://places.googleapis.com/v1/places:searchNearby";
  private static final int MAX_PHOTO_URL_COUNT = 7;
  private static final int GOOGLE_NEARBY_MAX_RESULT_COUNT = 20;
  private static final int CATEGORY_SEARCH_MAX_RESULT_COUNT = 100;
  private static final String AUTOCOMPLETE_FIELD_MASK =
      "suggestions.placePrediction.placeId,"
          + "suggestions.placePrediction.text.text,"
          + "suggestions.placePrediction.structuredFormat.mainText.text,"
          + "suggestions.placePrediction.structuredFormat.secondaryText.text";
  private static final String PLACE_RESULT_FIELD_MASK =
      "places.id,"
          + "places.displayName,places.types,"
          + "places.formattedAddress,"
          + "places.location,"
          + "places.accessibilityOptions,"
          + "places.regularOpeningHours.openNow,places.regularOpeningHours.weekdayDescriptions,"
          + "places.nationalPhoneNumber,"
          + "places.userRatingCount,"
          + "places.photos.name";
  private static final String PLACE_FIELD_MASK = PLACE_RESULT_FIELD_MASK + ",nextPageToken";
  // Nearby/category and AI-course candidate searches do not render contact data or opening hours.
  // Keeping this mask at Pro prevents those background searches from being billed as Enterprise.
  private static final String NEARBY_PRO_FIELD_MASK =
      "places.id,"
          + "places.displayName,places.types,"
          + "places.formattedAddress,places.location,"
          + "places.accessibilityOptions,places.photos.name";

  @Value("${google.places.api-key}")
  private String googleApiKey;

  private final WebClient webClient;
  private final PlaceTestService placeTestService;
  private final PlaceSearchHistoryService placeSearchHistoryService;
  private final TourBarrierFreeService tourBarrierFreeService;

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

    GoogleAutocompleteResponseDto googleResponse = requestGoogleAutocomplete(requestBody);

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

  public PlaceDetailResponse getDetail(String placeId) {
    if (placeId == null || placeId.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (placeId.startsWith("PUBLIC_DATA:")) {
      String contentId = placeId.substring("PUBLIC_DATA:".length());
      PublicBarrierFreePlace publicPlace = tourBarrierFreeService.findByContentId(contentId);
      if (publicPlace == null) {
        throw new CustomException(ErrorCode.FACILITY_NOT_FOUND);
      }

      PublicBarrierFreeInfo publicInfo = publicPlace.barrierFreeInfo();
      return new PlaceDetailResponse(
          placeId,
          publicPlace.name(),
          publicPlace.address(),
          publicPlace.latitude(),
          publicPlace.longitude(),
          PlaceCategory.ETC,
          PlaceCategory.ETC.getLabel(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          publicInfo.elevator(),
          publicInfo.ramp(),
          publicInfo.voiceGuidance(),
          publicInfo.brailleBlock(),
          publicInfo.hearingSupport(),
          publicInfo.strollerRental(),
          publicInfo.nursingRoom(),
          publicInfo.wheelchairRental(),
          publicInfo.signLanguage(),
          publicInfo.restArea(),
          publicInfo.subtitleService(),
          publicInfo.sourceStatus(),
          null);
    }

    GooglePlaceResponseDto.Place place;
    try {
      place = placeTestService.getPlaceDetails(placeId);
    } catch (RuntimeException e) {
      throw toGoogleMapException("Details", Map.of("placeId", placeId), e);
    }
    if (place == null) {
      throw new CustomException(ErrorCode.FACILITY_NOT_FOUND);
    }

    GooglePlaceResponseDto.Location location = place.getLocation();
    GooglePlaceResponseDto.AccessibilityOptions accessibility = place.getAccessibilityOptions();
    GooglePlaceResponseDto.OpeningHours openingHours = place.getRegularOpeningHours();
    String name = place.getDisplayName() == null ? null : place.getDisplayName().getText();
    PlaceCategory category = PlaceCategory.inferFromTypes(place.getTypes());
    PublicBarrierFreeInfo publicInfo =
        tourBarrierFreeService.findByPlaceNameAndLocation(
            name,
            location == null ? null : location.getLatitude(),
            location == null ? null : location.getLongitude());

    Integer reviewCount = getReviewCount(place);

    return new PlaceDetailResponse(
        place.getId(),
        name,
        place.getFormattedAddress(),
        location == null ? null : location.getLatitude(),
        location == null ? null : location.getLongitude(),
        category,
        category.getLabel(),
        place.getNationalPhoneNumber(),
        place.getWebsiteUri(),
        openingHours == null ? null : openingHours.getOpenNow(),
        openingHours == null ? null : openingHours.getWeekdayDescriptions(),
        reviewCount,
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleEntrance(),
            publicInfo.ramp()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleParking(),
            publicInfo.accessibleParking()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleRestroom(),
            publicInfo.accessibleRestroom()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleSeating(),
            publicInfo.wheelchairSeat()),
        publicInfo.elevator(),
        publicInfo.ramp(),
        publicInfo.voiceGuidance(),
        publicInfo.brailleBlock(),
        publicInfo.hearingSupport(),
        publicInfo.strollerRental(),
        publicInfo.nursingRoom(),
        publicInfo.wheelchairRental(),
        publicInfo.signLanguage(),
        publicInfo.restArea(),
        publicInfo.subtitleService(),
        resolveAccessibilityDataSource(publicInfo, false),
        buildPhotoUrl(place));
  }

  public PlaceSearchResponse search(
      String keyword,
      String categoryValue,
      Double lat,
      Double lng,
      Integer radius,
      Integer pageSize,
      String pageToken,
      List<MobilityType> userTypes,
      List<FacilityType> facilities) {
    validateKeyword(keyword);
    PlaceCategory category = PlaceCategory.from(categoryValue);
    int normalizedPageSize = normalizePageSize(pageSize);
    boolean accessibilityFilterRequested = hasAccessibilityFilter(userTypes, facilities);
    Map<String, PublicBarrierFreeInfo> publicInfoCache = new HashMap<>();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("textQuery", keyword);
    requestBody.put("languageCode", "ko");
    requestBody.put("regionCode", "KR");
    requestBody.put("pageSize", normalizedPageSize);

    String includedType = resolveIncludedType(category);
    if (includedType != null) {
      requestBody.put("includedType", includedType);
    }
    if (pageToken != null && !pageToken.isBlank()) {
      requestBody.put("pageToken", pageToken);
    }
    putLocationBias(requestBody, lat, lng, radius);

    // A user search must result in exactly one billable Text Search request. Do not seed the
    // search with Autocomplete candidates or scan additional pages on behalf of the client.
    GooglePlaceResponseDto googleResponse = requestGoogleTextSearch(requestBody);
    List<PlaceSearchResponse.PlaceSummary> pagePlaces =
        googleResponse == null || googleResponse.getPlaces() == null
            ? List.of()
            : googleResponse.getPlaces().stream()
                .map(
                    place ->
                        toPlaceSummary(
                            place, category, accessibilityFilterRequested, publicInfoCache))
                .filter(place -> isWithinSearchArea(place, lat, lng, radius))
                .filter(place -> matchesUserTypes(place, userTypes))
                .filter(place -> matchesFacilities(place, facilities))
                .limit(normalizedPageSize)
                .toList();

    if (pageToken == null || pageToken.isBlank()) {
      placeSearchHistoryService.save(keyword, category, lat, lng, radius);
    }

    String nextPageToken = googleResponse == null ? null : googleResponse.getNextPageToken();

    return new PlaceSearchResponse(
        pagePlaces, nextPageToken, nextPageToken != null && !nextPageToken.isBlank());
  }

  /**
   * Returns one Pro-tier Nearby Search result set for AI course candidate selection. The course
   * service owns the total request budget and must not call the general Text Search flow.
   */
  public PlaceSearchResponse searchCourseCandidates(
      PlaceCategory category, double lat, double lng, int radius, List<MobilityType> userTypes) {
    validateMapSearchArea(lat, lng, radius);
    if (category == null || category == PlaceCategory.ETC) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    boolean accessibilityFilterRequested = hasAccessibilityFilter(userTypes, List.of());
    Map<String, PublicBarrierFreeInfo> publicInfoCache = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("languageCode", "ko");
    requestBody.put("regionCode", "KR");
    requestBody.put("rankPreference", "DISTANCE");
    requestBody.put("includedTypes", category.getGoogleTypes());
    requestBody.put("maxResultCount", GOOGLE_NEARBY_MAX_RESULT_COUNT);
    putLocationRestriction(requestBody, lat, lng, radius);

    GooglePlaceResponseDto googleResponse =
        requestGoogleNearbySearch(requestBody, NEARBY_PRO_FIELD_MASK);
    List<PlaceSearchResponse.PlaceSummary> places =
        googleResponse == null || googleResponse.getPlaces() == null
            ? List.of()
            : googleResponse.getPlaces().stream()
                .map(
                    place ->
                        toPlaceSummary(
                            place, category, accessibilityFilterRequested, publicInfoCache))
                .filter(place -> isWithinSearchArea(place, lat, lng, radius))
                .limit(GOOGLE_NEARBY_MAX_RESULT_COUNT)
                .toList();
    return new PlaceSearchResponse(places, null, false);
  }

  public PlaceSearchResponse searchByCategory(
      String categoryValue,
      Double lat,
      Double lng,
      Integer radius,
      Integer pageSize,
      List<MobilityType> userTypes,
      List<FacilityType> facilities) {
    validateMapSearchArea(lat, lng, radius);
    PlaceCategory category = PlaceCategory.from(categoryValue);
    if (category == PlaceCategory.ETC) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    int limit = normalizeCategorySearchLimit(pageSize);
    boolean accessibilityFilterRequested = hasAccessibilityFilter(userTypes, facilities);
    Map<String, PublicBarrierFreeInfo> publicInfoCache = new HashMap<>();
    List<PlaceSearchResponse.PlaceSummary> places = new ArrayList<>();

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("languageCode", "ko");
    requestBody.put("regionCode", "KR");
    requestBody.put("rankPreference", "DISTANCE");
    requestBody.put("includedTypes", category.getGoogleTypes());
    requestBody.put("maxResultCount", Math.min(GOOGLE_NEARBY_MAX_RESULT_COUNT, limit));
    putLocationRestriction(requestBody, lat, lng, radius);

    GooglePlaceResponseDto googleResponse =
        requestGoogleNearbySearch(requestBody, NEARBY_PRO_FIELD_MASK);
    if (googleResponse == null || googleResponse.getPlaces() == null) {
      return new PlaceSearchResponse(List.of(), null, false);
    }

    for (GooglePlaceResponseDto.Place place : googleResponse.getPlaces()) {
      if (places.size() >= limit) {
        break;
      }

      PlaceSearchResponse.PlaceSummary summary =
          toPlaceSummary(place, category, accessibilityFilterRequested, publicInfoCache);
      if (matchesUserTypes(summary, userTypes)
          && matchesFacilities(summary, facilities)
          && isWithinSearchArea(summary, lat, lng, radius)
          && !containsSamePlace(places, summary)
          && !containsPlaceId(places, summary.placeId())) {
        places.add(summary);
      }
    }

    return new PlaceSearchResponse(places, null, false);
  }

  public ResponseEntity<byte[]> getPhoto(String photoName, Integer maxWidthPx) {
    String normalizedPhotoName = normalizePhotoName(photoName);
    String photoUri = resolvePhotoUrl(normalizedPhotoName, maxWidthPx);

    ResponseEntity<byte[]> imageResponse;
    try {
      imageResponse = webClient.get().uri(photoUri).retrieve().toEntity(byte[].class).block();
    } catch (RuntimeException e) {
      throw toGoogleMapException("Photo Download", Map.of("photoName", normalizedPhotoName), e);
    }
    if (imageResponse == null || imageResponse.getBody() == null) {
      throw new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
    }

    MediaType contentType = imageResponse.getHeaders().getContentType();
    return ResponseEntity.status(imageResponse.getStatusCode())
        .contentType(contentType == null ? MediaType.IMAGE_JPEG : contentType)
        .body(imageResponse.getBody());
  }

  private GooglePlaceResponseDto requestGoogleTextSearch(Map<String, Object> requestBody) {
    try {
      return webClient
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
    } catch (RuntimeException e) {
      throw toGoogleMapException("Text Search", requestBody, e);
    }
  }

  private GooglePlaceResponseDto requestGoogleNearbySearch(Map<String, Object> requestBody) {
    return requestGoogleNearbySearch(requestBody, NEARBY_PRO_FIELD_MASK);
  }

  private GooglePlaceResponseDto requestGoogleNearbySearch(
      Map<String, Object> requestBody, String fieldMask) {
    try {
      return webClient
          .post()
          .uri(NEARBY_SEARCH_URL)
          .header("X-Goog-Api-Key", googleApiKey)
          .header("X-Goog-FieldMask", fieldMask)
          .bodyValue(requestBody)
          .retrieve()
          .onStatus(
              HttpStatusCode::isError,
              response -> {
                log.error("Google Places Nearby Search failed. status={}", response.statusCode());
                return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
              })
          .bodyToMono(GooglePlaceResponseDto.class)
          .block();
    } catch (RuntimeException e) {
      throw toGoogleMapException("Nearby Search", requestBody, e);
    }
  }

  private GoogleAutocompleteResponseDto requestGoogleAutocomplete(Map<String, Object> requestBody) {
    try {
      return webClient
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
    } catch (RuntimeException e) {
      throw toGoogleMapException("Autocomplete", requestBody, e);
    }
  }

  private boolean hasAccessibilityFilter(
      List<MobilityType> userTypes, List<FacilityType> facilities) {
    return (userTypes != null && !userTypes.isEmpty())
        || (facilities != null && !facilities.isEmpty());
  }

  private boolean isWithinSearchArea(
      PlaceSearchResponse.PlaceSummary place, Double lat, Double lng, Integer radius) {
    return isWithinSearchArea(place.lat(), place.lng(), lat, lng, radius);
  }

  private boolean isWithinSearchArea(
      Double placeLat, Double placeLng, Double centerLat, Double centerLng, Integer radius) {
    if (centerLat == null || centerLng == null) {
      return true;
    }
    if (placeLat == null || placeLng == null) {
      return false;
    }

    double meters = radius == null ? 500.0 : radius.doubleValue();
    double earthRadius = 6371000.0;
    double lat1 = Math.toRadians(centerLat);
    double lat2 = Math.toRadians(placeLat);
    double deltaLat = Math.toRadians(placeLat - centerLat);
    double deltaLng = Math.toRadians(placeLng - centerLng);
    double a =
        Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c <= meters;
  }

  private boolean matchesUserTypes(
      PlaceSearchResponse.PlaceSummary place, List<MobilityType> userTypes) {
    if (userTypes == null || userTypes.isEmpty()) {
      return true;
    }

    for (MobilityType userType : userTypes) {
      if (userType == null) {
        continue;
      }
      boolean matched =
          switch (userType) {
            case WHEELCHAIR ->
                Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleParking())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleRestroom())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleSeating())
                    || Boolean.TRUE.equals(place.ramp())
                    || Boolean.TRUE.equals(place.elevator());
            case STROLLER ->
                Boolean.TRUE.equals(place.strollerRental())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                    || Boolean.TRUE.equals(place.elevator());
            case COGNITIVE_DEVELOPMENTAL ->
                Boolean.TRUE.equals(place.restArea())
                    || Boolean.TRUE.equals(place.nursingRoom())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                    || Boolean.TRUE.equals(place.wheelchairAccessibleRestroom())
                    || Boolean.TRUE.equals(place.elevator());
            case VISUAL_IMPAIRMENT ->
                Boolean.TRUE.equals(place.voiceGuidance())
                    || Boolean.TRUE.equals(place.brailleBlock());
            case HEARING_IMPAIRMENT ->
                Boolean.TRUE.equals(place.signLanguage())
                    || Boolean.TRUE.equals(place.subtitleService())
                    || Boolean.TRUE.equals(place.hearingSupport());
            case OTHER -> false;
          };
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesFacilities(
      PlaceSearchResponse.PlaceSummary place, List<FacilityType> facilities) {
    if (facilities == null || facilities.isEmpty()) {
      return true;
    }

    for (FacilityType facility : facilities) {
      if (facility == null) {
        continue;
      }
      boolean matched =
          switch (facility) {
            case ELEVATOR -> Boolean.TRUE.equals(place.elevator());
            case ACCESSIBLE_PARKING -> Boolean.TRUE.equals(place.wheelchairAccessibleParking());
            case WHEELCHAIR_SEAT -> Boolean.TRUE.equals(place.wheelchairAccessibleSeating());
            case ACCESSIBLE_RESTROOM -> Boolean.TRUE.equals(place.wheelchairAccessibleRestroom());
            case RAMP -> Boolean.TRUE.equals(place.ramp());
            case VOICE_GUIDANCE -> Boolean.TRUE.equals(place.voiceGuidance());
            case BRAILLE_BLOCK -> Boolean.TRUE.equals(place.brailleBlock());
            case ELECTRIC_WHEELCHAIR_RENTAL -> Boolean.TRUE.equals(place.wheelchairRental());
            case SIGN_LANGUAGE_INTERPRETATION -> Boolean.TRUE.equals(place.signLanguage());
            case REST_AREA -> Boolean.TRUE.equals(place.restArea());
            case NURSING_ROOM -> Boolean.TRUE.equals(place.nursingRoom());
            case SUBTITLE_SERVICE -> Boolean.TRUE.equals(place.subtitleService());
            case NONE -> false;
          };
      if (matched) {
        return true;
      }
    }
    return false;
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
      GooglePlaceResponseDto.Place place,
      PlaceCategory category,
      boolean accessibilityFilterRequested,
      Map<String, PublicBarrierFreeInfo> publicInfoCache) {
    category = resolveResponseCategory(place, category);
    GooglePlaceResponseDto.Location location = place.getLocation();
    GooglePlaceResponseDto.AccessibilityOptions accessibility = place.getAccessibilityOptions();
    GooglePlaceResponseDto.OpeningHours openingHours = place.getRegularOpeningHours();
    String name = place.getDisplayName() == null ? null : place.getDisplayName().getText();
    Integer reviewCount = getReviewCount(place);
    PublicBarrierFreeInfo publicInfo =
        accessibilityFilterRequested
            ? getPublicInfo(
                name,
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                publicInfoCache)
            : PublicBarrierFreeInfo.empty();

    List<String> photoUrls = buildPhotoUrls(place);

    return new PlaceSearchResponse.PlaceSummary(
        place.getId(),
        name,
        place.getFormattedAddress(),
        location == null ? null : location.getLatitude(),
        location == null ? null : location.getLongitude(),
        category,
        category.getLabel(),
        place.getNationalPhoneNumber(),
        openingHours == null ? null : openingHours.getOpenNow(),
        openingHours == null ? null : openingHours.getWeekdayDescriptions(),
        reviewCount,
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleEntrance(),
            publicInfo.ramp()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleParking(),
            publicInfo.accessibleParking()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleRestroom(),
            publicInfo.accessibleRestroom()),
        merge(
            accessibility == null ? null : accessibility.getWheelchairAccessibleSeating(),
            publicInfo.wheelchairSeat()),
        publicInfo.elevator(),
        publicInfo.ramp(),
        publicInfo.voiceGuidance(),
        publicInfo.brailleBlock(),
        publicInfo.hearingSupport(),
        publicInfo.strollerRental(),
        publicInfo.nursingRoom(),
        publicInfo.wheelchairRental(),
        publicInfo.signLanguage(),
        publicInfo.restArea(),
        publicInfo.subtitleService(),
        resolveAccessibilityDataSource(publicInfo, accessibilityFilterRequested),
        photoUrls.isEmpty() ? null : photoUrls.getFirst(),
        photoUrls);
  }

  private PlaceSearchResponse.PlaceSummary toPlaceSummary(
      PublicBarrierFreePlace place, PlaceCategory category, boolean accessibilityFilterRequested) {
    PublicBarrierFreeInfo publicInfo = place.barrierFreeInfo();

    return new PlaceSearchResponse.PlaceSummary(
        "PUBLIC_DATA:" + place.contentId(),
        place.name(),
        place.address(),
        place.latitude(),
        place.longitude(),
        category,
        category.getLabel(),
        null,
        null,
        null,
        null,
        publicInfo.ramp(),
        publicInfo.accessibleParking(),
        publicInfo.accessibleRestroom(),
        publicInfo.wheelchairSeat(),
        publicInfo.elevator(),
        publicInfo.ramp(),
        publicInfo.voiceGuidance(),
        publicInfo.brailleBlock(),
        publicInfo.hearingSupport(),
        publicInfo.strollerRental(),
        publicInfo.nursingRoom(),
        publicInfo.wheelchairRental(),
        publicInfo.signLanguage(),
        publicInfo.restArea(),
        publicInfo.subtitleService(),
        resolveAccessibilityDataSource(publicInfo, accessibilityFilterRequested),
        null,
        null);
  }

  private String buildPhotoUrl(GooglePlaceResponseDto.Place place) {
    List<String> photoUrls = buildPhotoUrls(place);
    return photoUrls.isEmpty() ? null : photoUrls.getFirst();
  }

  private List<String> buildPhotoUrls(GooglePlaceResponseDto.Place place) {
    if (place == null || place.getPhotos() == null || place.getPhotos().isEmpty()) {
      return List.of();
    }

    return place.getPhotos().stream()
        .map(GooglePlaceResponseDto.Photo::getName)
        .filter(photoName -> photoName != null && !photoName.isBlank())
        .limit(MAX_PHOTO_URL_COUNT)
        .map(
            photoName ->
                UriComponentsBuilder.fromPath("/api/v1/places/photos")
                    .queryParam("name", photoName)
                    .queryParam("maxWidthPx", 800)
                    .build()
                    .encode()
                    .toUriString())
        .toList();
  }

  private Integer getReviewCount(GooglePlaceResponseDto.Place place) {
    if (place == null) {
      return null;
    }
    if (place.getUserRatingCount() != null) {
      return place.getUserRatingCount();
    }
    return place.getReviews() == null ? null : place.getReviews().size();
  }

  private String resolveIncludedType(PlaceCategory category) {
    if (category == null || category == PlaceCategory.ETC) {
      return null;
    }
    return category.getPrimaryGoogleType();
  }

  private String resolvePhotoUrl(String photoName, Integer maxWidthPx) {
    validatePhotoName(photoName);
    int normalizedMaxWidthPx = normalizePhotoWidth(maxWidthPx);

    String metadataUrl;
    try {
      metadataUrl =
          UriComponentsBuilder.fromUriString("https://places.googleapis.com/v1")
              .pathSegment(photoName.split("/"))
              .pathSegment("media")
              .queryParam("maxWidthPx", normalizedMaxWidthPx)
              .queryParam("skipHttpRedirect", true)
              .queryParam("key", googleApiKey)
              .build()
              .toUriString();
    } catch (RuntimeException e) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    JsonNode photoMetadata;
    try {
      photoMetadata =
          webClient
              .get()
              .uri(metadataUrl)
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  response ->
                      response
                          .bodyToMono(String.class)
                          .defaultIfEmpty("")
                          .flatMap(
                              errorBody -> {
                                log.error(
                                    "Google Places Photo metadata failed. status={}, body={}, photoName={}",
                                    response.statusCode(),
                                    errorBody,
                                    photoName);
                                return Mono.error(
                                    new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
                              }))
              .bodyToMono(JsonNode.class)
              .block();
    } catch (RuntimeException e) {
      throw toGoogleMapException("Photo Metadata", Map.of("photoName", photoName), e);
    }

    String photoUri = photoMetadata == null ? null : photoMetadata.path("photoUri").asText(null);
    if (photoUri == null || photoUri.isBlank()) {
      log.error(
          "Google Places Photo metadata did not contain photoUri. photoName={}, metadata={}",
          photoName,
          photoMetadata);
      throw new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
    }
    return photoUri;
  }

  private String normalizePhotoName(String photoName) {
    if (photoName == null) {
      return null;
    }
    String normalized = photoName.trim();
    if (normalized.contains("%2F") || normalized.contains("%2f")) {
      normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
    }
    return normalized;
  }

  private void validatePhotoName(String photoName) {
    if (photoName == null
        || photoName.isBlank()
        || !photoName.startsWith("places/")
        || !photoName.contains("/photos/")
        || photoName.contains("..")
        || photoName.contains("?")
        || photoName.contains("#")
        || photoName.contains("http")) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private int normalizePhotoWidth(Integer maxWidthPx) {
    if (maxWidthPx == null) {
      return 800;
    }
    return Math.max(1, Math.min(maxWidthPx, 1600));
  }

  private PublicBarrierFreeInfo getPublicInfo(
      String name,
      Double latitude,
      Double longitude,
      Map<String, PublicBarrierFreeInfo> publicInfoCache) {
    String cacheKey = buildPublicInfoCacheKey(name, latitude, longitude);
    if (cacheKey.isBlank()) {
      return PublicBarrierFreeInfo.empty();
    }
    return publicInfoCache.computeIfAbsent(
        cacheKey,
        key -> tourBarrierFreeService.findByPlaceNameAndLocation(name, latitude, longitude));
  }

  private String buildPublicInfoCacheKey(String name, Double latitude, Double longitude) {
    String normalizedName = normalize(name);
    if (normalizedName.isBlank()) {
      return "";
    }
    if (latitude == null || longitude == null) {
      return normalizedName;
    }
    return normalizedName
        + ":"
        + Math.round(latitude * 10000)
        + ":"
        + Math.round(longitude * 10000);
  }

  private String resolveAccessibilityDataSource(
      PublicBarrierFreeInfo publicInfo, boolean accessibilityFilterRequested) {
    if (publicInfo.hasAnyValue()) {
      return publicInfo.sourceStatus();
    }
    return accessibilityFilterRequested ? publicInfo.sourceStatus() : "GOOGLE_PLACES_ONLY";
  }

  private boolean hasPublicData(PlaceSearchResponse.PlaceSummary place) {
    return "GOOGLE_PLACES_AND_PUBLIC_DATA".equals(place.accessibilityDataSource());
  }

  private boolean containsSamePlace(
      List<PlaceSearchResponse.PlaceSummary> places, PlaceSearchResponse.PlaceSummary target) {
    return places.stream().anyMatch(place -> isSamePlace(place, target));
  }

  private boolean containsPlaceId(List<PlaceSearchResponse.PlaceSummary> places, String placeId) {
    if (placeId == null || placeId.isBlank()) {
      return false;
    }
    return places.stream().anyMatch(place -> placeId.equals(place.placeId()));
  }

  private PlaceCategory resolveResponseCategory(
      GooglePlaceResponseDto.Place place, PlaceCategory requestedCategory) {
    // 음식점/카페는 검색 요청이 아니라 제공된 실제 유형으로 구분합니다.
    if ((requestedCategory == PlaceCategory.FOOD || requestedCategory == PlaceCategory.CAFE)
        && place.getTypes() != null
        && !place.getTypes().isEmpty()) {
      PlaceCategory inferredCategory = PlaceCategory.inferFromTypes(place.getTypes());
      return inferredCategory == PlaceCategory.ETC ? requestedCategory : inferredCategory;
    }
    if (requestedCategory != null && requestedCategory != PlaceCategory.ETC) {
      return requestedCategory;
    }
    return PlaceCategory.inferFromTypes(place.getTypes());
  }

  private boolean isSamePlace(
      PlaceSearchResponse.PlaceSummary left, PlaceSearchResponse.PlaceSummary right) {
    String leftName = normalize(left.name());
    String rightName = normalize(right.name());
    if (leftName.isBlank() || rightName.isBlank()) {
      return false;
    }
    return leftName.equals(rightName)
        || leftName.contains(rightName)
        || rightName.contains(leftName);
  }

  private String normalize(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
  }

  private Boolean merge(Boolean googleValue, Boolean publicValue) {
    if (Boolean.TRUE.equals(googleValue) || Boolean.TRUE.equals(publicValue)) {
      return true;
    }
    if (Boolean.FALSE.equals(googleValue) || Boolean.FALSE.equals(publicValue)) {
      return false;
    }
    return null;
  }

  private CustomException toGoogleMapException(
      String operation, Map<String, Object> requestBody, RuntimeException exception) {
    Throwable unwrapped = Exceptions.unwrap(exception);
    if (unwrapped instanceof CustomException customException) {
      return customException;
    }
    if (unwrapped instanceof WebClientResponseException responseException) {
      log.error(
          "Google Places {} failed. status={}, body={}, request={}",
          operation,
          responseException.getStatusCode(),
          responseException.getResponseBodyAsString(),
          sanitizeRequestBody(requestBody),
          responseException);
      return new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
    }
    if (isTimeoutException(unwrapped)) {
      log.error(
          "Google Places {} timed out. request={}",
          operation,
          sanitizeRequestBody(requestBody),
          exception);
      return new CustomException(ErrorCode.GOOGLE_MAP_TIMEOUT);
    }

    log.error(
        "Google Places {} failed unexpectedly. request={}",
        operation,
        sanitizeRequestBody(requestBody),
        exception);
    return new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
  }

  private boolean isTimeoutException(Throwable throwable) {
    while (throwable != null) {
      String className = throwable.getClass().getName().toLowerCase();
      if (className.contains("timeout")) {
        return true;
      }
      throwable = throwable.getCause();
    }
    return false;
  }

  private Map<String, Object> sanitizeRequestBody(Map<String, Object> requestBody) {
    Map<String, Object> sanitized = new HashMap<>(requestBody);
    sanitized.remove("pageToken");
    return sanitized;
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

  private void putLocationRestriction(
      Map<String, Object> requestBody, Double lat, Double lng, Integer radius) {
    Map<String, Object> center = new HashMap<>();
    center.put("latitude", lat);
    center.put("longitude", lng);

    Map<String, Object> circle = new HashMap<>();
    circle.put("center", center);
    circle.put("radius", radius.doubleValue());

    requestBody.put("locationRestriction", Map.of("circle", circle));
  }

  private void validateKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private void validateMapSearchArea(Double lat, Double lng, Integer radius) {
    if (!isLatitude(lat) || !isLongitude(lng) || radius == null || radius < 1 || radius > 50000) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private boolean isLatitude(Double value) {
    return value != null && Double.isFinite(value) && value >= -90.0 && value <= 90.0;
  }

  private boolean isLongitude(Double value) {
    return value != null && Double.isFinite(value) && value >= -180.0 && value <= 180.0;
  }

  private int normalizePageSize(Integer pageSize) {
    if (pageSize == null) {
      return 20;
    }
    if (pageSize < 1) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return Math.min(pageSize, 20);
  }

  private int normalizeCategorySearchLimit(Integer pageSize) {
    if (pageSize == null) {
      return CATEGORY_SEARCH_MAX_RESULT_COUNT;
    }
    if (pageSize < 1) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return Math.min(pageSize, CATEGORY_SEARCH_MAX_RESULT_COUNT);
  }
}
