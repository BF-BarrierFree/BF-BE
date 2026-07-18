package com.barrierfree.bf.place.domain;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum PlaceCategory {
  FOOD_CAFE("음식점/카페", List.of("restaurant", "cafe", "bakery", "bar", "meal_takeaway")),
  TOUR_CULTURE(
      "관광지/문화시설",
      List.of(
          "tourist_attraction",
          "museum",
          "art_gallery",
          "cultural_landmark",
          "performing_arts_theater")),
  PARK_TRAIL("공원/산책로", List.of("park", "hiking_area", "garden", "national_park", "scenic_spot")),
  LODGING("숙박", List.of("lodging", "hotel", "motel", "guest_house", "hostel")),
  TRANSPORTATION(
      "교통",
      List.of("transit_station", "bus_station", "subway_station", "train_station", "airport")),
  PUBLIC_FACILITY(
      "공공시설",
      List.of(
          "government_office", "local_government_office", "city_hall", "post_office", "police")),
  ETC("기타", List.of());

  private final String label;
  private final List<String> googleTypes;

  PlaceCategory(String label, List<String> googleTypes) {
    this.label = label;
    this.googleTypes = googleTypes;
  }

  public String getLabel() {
    return label;
  }

  public List<String> getGoogleTypes() {
    return googleTypes;
  }

  public String getPrimaryGoogleType() {
    return googleTypes.isEmpty() ? null : googleTypes.get(0);
  }

  public static PlaceCategory from(String value) {
    if (value == null || value.isBlank()) {
      return ETC;
    }

    String normalized = normalize(value);
    return Arrays.stream(values())
        .filter(category -> normalize(category.name()).equals(normalized))
        .findFirst()
        .or(
            () ->
                Arrays.stream(values())
                    .filter(category -> normalize(category.label).equals(normalized))
                    .findFirst())
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }

  public static PlaceCategory inferFromTypes(List<String> types) {
    if (types == null || types.isEmpty()) {
      return ETC;
    }

    Set<String> typeSet = Set.copyOf(types);
    return Arrays.stream(values())
        .filter(category -> category != ETC)
        .filter(category -> category.googleTypes.stream().anyMatch(typeSet::contains))
        .findFirst()
        .orElse(ETC);
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-/]", "");
  }
}
