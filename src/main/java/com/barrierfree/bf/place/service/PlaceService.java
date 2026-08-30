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
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.web.util.UriComponentsBuilder;
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
          + "places.accessibilityOptions,"
          + "places.regularOpeningHours,"
          + "places.photos,"
          + "nextPageToken";

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

    GooglePlaceResponseDto.Place place = placeTestService.getPlaceDetails(placeId);
    if (place == null) {
      throw new CustomException(ErrorCode.FACILITY_NOT_FOUND);
    }

    GooglePlaceResponseDto.Location location = place.getLocation();
    GooglePlaceResponseDto.AccessibilityOptions accessibility = place.getAccessibilityOptions();
    GooglePlaceResponseDto.OpeningHours openingHours = place.getRegularOpeningHours();
    String name = place.getDisplayName() == null ? null : place.getDisplayName().getText();
    PlaceCategory category = PlaceCategory.inferFromTypes(place.getTypes());
    PublicBarrierFreeInfo publicInfo = tourBarrierFreeService.findByPlaceName(name);

    Integer reviewCount = place.getReviews() == null ? null : place.getReviews().size();

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

    List<PlaceSearchResponse.PlaceSummary> places = new ArrayList<>();
    List<PlaceSearchResponse.PlaceSummary> fallbackPlaces = new ArrayList<>();
    GooglePlaceResponseDto googleResponse = null;
    String nextRequestPageToken = pageToken;

    if (pageToken == null || pageToken.isBlank()) {
      for (String candidateQuery :
          buildCandidateQueries(keyword, categoryValue, lat, lng, radius)) {
        if (places.size() >= normalizedPageSize) {
          break;
        }

        List<PlaceSearchResponse.PlaceSummary> candidatePlaces = new ArrayList<>();

        for (PlaceSearchResponse.PlaceSummary summary :
            searchPublicExactCandidate(
                candidateQuery, category, lat, lng, radius, accessibilityFilterRequested)) {
          if (candidatePlaces.size() >= normalizedPageSize) {
            break;
          }
          if (matchesUserTypes(summary, userTypes)
              && matchesFacilities(summary, facilities)
              && isWithinSearchArea(summary, lat, lng, radius)
              && !containsSamePlace(candidatePlaces, summary)) {
            candidatePlaces.add(summary);
          }
        }

        if (candidatePlaces.isEmpty()) {
          for (PlaceSearchResponse.PlaceSummary summary :
              searchGoogleExactCandidate(
                  candidateQuery,
                  category,
                  lat,
                  lng,
                  radius,
                  accessibilityFilterRequested,
                  publicInfoCache)) {
            if (candidatePlaces.size() >= normalizedPageSize) {
              break;
            }
            if (matchesUserTypes(summary, userTypes)
                && matchesFacilities(summary, facilities)
                && isWithinSearchArea(summary, lat, lng, radius)
                && !containsSamePlace(candidatePlaces, summary)) {
              candidatePlaces.add(summary);
            }
          }
        }

        for (PlaceSearchResponse.PlaceSummary summary : candidatePlaces) {
          if (places.size() >= normalizedPageSize) {
            break;
          }
          if (!containsSamePlace(places, summary)) {
            places.add(summary);
          }
        }
      }
    }

    while (places.size() < normalizedPageSize) {
      if (nextRequestPageToken != null && !nextRequestPageToken.isBlank()) {
        requestBody.put("pageToken", nextRequestPageToken);
      } else {
        requestBody.remove("pageToken");
      }

      googleResponse = requestGoogleTextSearch(requestBody);

      if (googleResponse != null && googleResponse.getPlaces() != null) {
        List<PlaceSearchResponse.PlaceSummary> pagePlaces =
            googleResponse.getPlaces().stream()
                .map(
                    place ->
                        toPlaceSummary(
                            place, category, accessibilityFilterRequested, publicInfoCache))
                .filter(place -> isWithinSearchArea(place, lat, lng, radius))
                .toList();

        pagePlaces.stream()
            .limit(Math.max(0, normalizedPageSize - fallbackPlaces.size()))
            .forEach(fallbackPlaces::add);

        if (accessibilityFilterRequested) {
          List<PlaceSearchResponse.PlaceSummary> filteredPlaces =
              pagePlaces.stream()
                  .filter(place -> matchesUserTypes(place, userTypes))
                  .filter(place -> matchesFacilities(place, facilities))
                  .toList();

          for (PlaceSearchResponse.PlaceSummary place : filteredPlaces) {
            if (places.size() >= normalizedPageSize) {
              break;
            }
            if (!containsSamePlace(places, place)) {
              places.add(place);
            }
          }
        } else {
          List<PlaceSearchResponse.PlaceSummary> publicBacked = new ArrayList<>();
          List<PlaceSearchResponse.PlaceSummary> others = new ArrayList<>();
          for (PlaceSearchResponse.PlaceSummary place : pagePlaces) {
            if (containsSamePlace(places, place)) {
              continue;
            }
            if (hasPublicData(place)) {
              publicBacked.add(place);
            } else {
              others.add(place);
            }
          }

          for (PlaceSearchResponse.PlaceSummary place : publicBacked) {
            if (places.size() >= normalizedPageSize) {
              break;
            }
            places.add(place);
          }

          for (PlaceSearchResponse.PlaceSummary place : others) {
            if (places.size() >= normalizedPageSize) {
              break;
            }
            places.add(place);
          }
        }
      }

      nextRequestPageToken = googleResponse == null ? null : googleResponse.getNextPageToken();
      if (!shouldScanNextPage(places.size(), normalizedPageSize, nextRequestPageToken)) {
        break;
      }
    }

    if (pageToken == null || pageToken.isBlank()) {
      placeSearchHistoryService.save(keyword, category, lat, lng, radius);
    }

    String nextPageToken = googleResponse == null ? null : googleResponse.getNextPageToken();
    if (places.isEmpty() && accessibilityFilterRequested) {
      return new PlaceSearchResponse(List.of(), null, false);
    }

    if (places.isEmpty()) {
      places = fallbackPlaces.stream().limit(normalizedPageSize).toList();
    } else if (!accessibilityFilterRequested && places.size() < normalizedPageSize) {
      for (PlaceSearchResponse.PlaceSummary fallbackPlace : fallbackPlaces) {
        if (places.size() >= normalizedPageSize) {
          break;
        }
        if (!containsSamePlace(places, fallbackPlace)) {
          places.add(fallbackPlace);
        }
      }
    }

    return new PlaceSearchResponse(
        places, nextPageToken, nextPageToken != null && !nextPageToken.isBlank());
  }

  public ResponseEntity<byte[]> getPhoto(String photoName, Integer maxWidthPx) {
    String photoUri = resolvePhotoUrl(photoName, maxWidthPx);

    ResponseEntity<byte[]> imageResponse =
        webClient.get().uri(photoUri).retrieve().toEntity(byte[].class).block();
    if (imageResponse == null || imageResponse.getBody() == null) {
      throw new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
    }

    MediaType contentType = imageResponse.getHeaders().getContentType();
    return ResponseEntity.status(imageResponse.getStatusCode())
        .contentType(contentType == null ? MediaType.IMAGE_JPEG : contentType)
        .body(imageResponse.getBody());
  }

  private GooglePlaceResponseDto requestGoogleTextSearch(Map<String, Object> requestBody) {
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
  }

  private List<String> buildCandidateQueries(
      String keyword, String categoryValue, Double lat, Double lng, Integer radius) {
    List<String> candidates = new ArrayList<>();
    try {
      PlaceAutocompleteResponse autocompleteResponse =
          autocomplete(keyword, categoryValue, lat, lng, radius);
      if (autocompleteResponse != null && autocompleteResponse.suggestions() != null) {
        for (PlaceAutocompleteResponse.Suggestion suggestion : autocompleteResponse.suggestions()) {
          if (suggestion != null && suggestion.name() != null && !suggestion.name().isBlank()) {
            addCandidateIfAbsent(candidates, suggestion.name());
          }
        }
      }
    } catch (RuntimeException e) {
      log.warn("Autocomplete seed lookup failed. keyword={}", keyword, e);
    }

    if (candidates.isEmpty()) {
      candidates.add(keyword);
    }
    return candidates;
  }

  private List<PlaceSearchResponse.PlaceSummary> searchPublicExactCandidate(
      String candidateQuery,
      PlaceCategory category,
      Double lat,
      Double lng,
      Integer radius,
      boolean accessibilityFilterRequested) {
    List<PublicBarrierFreePlace> publicPlaces =
        tourBarrierFreeService.searchByKeyword(candidateQuery, 5);
    if (publicPlaces.isEmpty()) {
      return List.of();
    }

    return publicPlaces.stream()
        .map(place -> toPlaceSummary(place, category, accessibilityFilterRequested))
        .filter(place -> isWithinSearchArea(place, lat, lng, radius))
        .toList();
  }

  private List<PlaceSearchResponse.PlaceSummary> searchGoogleExactCandidate(
      String candidateQuery,
      PlaceCategory category,
      Double lat,
      Double lng,
      Integer radius,
      boolean accessibilityFilterRequested,
      Map<String, PublicBarrierFreeInfo> publicInfoCache) {
    Map<String, Object> candidateRequestBody = new HashMap<>();
    candidateRequestBody.put("textQuery", candidateQuery);
    candidateRequestBody.put("languageCode", "ko");
    candidateRequestBody.put("regionCode", "KR");
    candidateRequestBody.put("pageSize", 5);
    String includedType = resolveIncludedType(category);
    if (includedType != null) {
      candidateRequestBody.put("includedType", includedType);
    }
    putLocationBias(candidateRequestBody, lat, lng, radius);

    GooglePlaceResponseDto googleResponse = requestGoogleTextSearch(candidateRequestBody);
    if (googleResponse == null || googleResponse.getPlaces() == null) {
      return List.of();
    }

    return googleResponse.getPlaces().stream()
        .map(
            place -> toPlaceSummary(place, category, accessibilityFilterRequested, publicInfoCache))
        .toList();
  }

  private void addCandidateIfAbsent(List<String> candidates, String candidate) {
    String normalizedCandidate = normalize(candidate);
    if (normalizedCandidate.isBlank()) {
      return;
    }
    boolean exists =
        candidates.stream().anyMatch(existing -> normalize(existing).equals(normalizedCandidate));
    if (!exists) {
      candidates.add(candidate);
    }
  }

  private boolean shouldScanNextPage(int resultCount, int pageSize, String nextPageToken) {
    return resultCount < pageSize && nextPageToken != null && !nextPageToken.isBlank();
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
    GooglePlaceResponseDto.Location location = place.getLocation();
    GooglePlaceResponseDto.AccessibilityOptions accessibility = place.getAccessibilityOptions();
    GooglePlaceResponseDto.OpeningHours openingHours = place.getRegularOpeningHours();
    String name = place.getDisplayName() == null ? null : place.getDisplayName().getText();
    PublicBarrierFreeInfo publicInfo = getPublicInfo(name, publicInfoCache);

    return new PlaceSearchResponse.PlaceSummary(
        place.getId(),
        name,
        place.getFormattedAddress(),
        location == null ? null : location.getLatitude(),
        location == null ? null : location.getLongitude(),
        category,
        category.getLabel(),
        openingHours == null ? null : openingHours.getOpenNow(),
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
        buildPhotoUrl(place));
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
        null);
  }

  private String buildPhotoUrl(GooglePlaceResponseDto.Place place) {
    if (place == null || place.getPhotos() == null || place.getPhotos().isEmpty()) {
      return null;
    }

    String photoName = place.getPhotos().getFirst().getName();
    if (photoName == null || photoName.isBlank()) {
      return null;
    }

    return UriComponentsBuilder.fromPath("/api/v1/places/photos")
        .queryParam("name", photoName)
        .queryParam("maxWidthPx", 800)
        .build()
        .encode()
        .toUriString();
  }

  private String resolveIncludedType(PlaceCategory category) {
    if (category == null || category == PlaceCategory.ETC || category == PlaceCategory.FOOD_CAFE) {
      return null;
    }
    return category.getPrimaryGoogleType();
  }

  private String resolvePhotoUrl(String photoName, Integer maxWidthPx) {
    validatePhotoName(photoName);
    int normalizedMaxWidthPx = normalizePhotoWidth(maxWidthPx);

    String metadataUrl =
        UriComponentsBuilder.fromUriString("https://places.googleapis.com/v1/" + photoName + "/media")
            .queryParam("maxWidthPx", normalizedMaxWidthPx)
            .queryParam("skipHttpRedirect", true)
            .queryParam("key", googleApiKey)
            .build()
            .toUriString();

    JsonNode photoMetadata =
        webClient
            .get()
            .uri(metadataUrl)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                response -> {
                  log.error("Google Places Photo metadata failed. status={}", response.statusCode());
                  return Mono.error(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));
                })
            .bodyToMono(JsonNode.class)
            .block();

    String photoUri = photoMetadata == null ? null : photoMetadata.path("photoUri").asText(null);
    if (photoUri == null || photoUri.isBlank()) {
      throw new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED);
    }
    return photoUri;
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
      String name, Map<String, PublicBarrierFreeInfo> publicInfoCache) {
    String cacheKey = normalize(name);
    if (cacheKey.isBlank()) {
      return PublicBarrierFreeInfo.empty();
    }
    return publicInfoCache.computeIfAbsent(
        cacheKey, key -> tourBarrierFreeService.findByPlaceName(name));
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

  private int normalizePageSize(Integer pageSize) {
    if (pageSize == null) {
      return 20;
    }
    if (pageSize < 1) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return Math.min(pageSize, 20);
  }
}
