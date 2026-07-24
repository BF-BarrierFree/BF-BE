package com.barrierfree.bf.place.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GooglePlaceResponseDto {

  private List<Place> places;
  private String nextPageToken;

  @Getter
  @NoArgsConstructor
  public static class Place {
    private String id;
    private LocalizedText displayName;
    private String formattedAddress;
    private Location location;
    private List<String> types;

    private List<Review> reviews;
    private LocalizedText editorialSummary;
    private GenerativeSummary generativeSummary;

    private AccessibilityOptions accessibilityOptions;

    private String nationalPhoneNumber;
    private String websiteUri;
    private Boolean allowsDogs;
    private ParkingOptions parkingOptions;
    private OpeningHours regularOpeningHours;
    private List<Photo> photos;
  }

  @Getter
  @NoArgsConstructor
  public static class LocalizedText {
    private String text;
    private String languageCode;
  }

  @Getter
  @NoArgsConstructor
  public static class Location {
    private Double latitude;
    private Double longitude;
  }

  @Getter
  @NoArgsConstructor
  public static class Review {
    private String relativePublishTimeDescription;
    private Integer rating;
    private LocalizedText text;
    private LocalizedText originalText;
  }

  @Getter
  @NoArgsConstructor
  public static class GenerativeSummary {
    private LocalizedText overview;
  }

  @Getter
  @NoArgsConstructor
  public static class AccessibilityOptions {
    private Boolean wheelchairAccessibleParking;
    private Boolean wheelchairAccessibleEntrance;
    private Boolean wheelchairAccessibleRestroom;
    private Boolean wheelchairAccessibleSeating;
  }

  @Getter
  @NoArgsConstructor
  public static class ParkingOptions {
    private Boolean freeParkingLot;
    private Boolean paidParkingLot;
    private Boolean valetParking;
  }

  @Getter
  @NoArgsConstructor
  public static class OpeningHours {
    private Boolean openNow;
    private List<String> weekdayDescriptions;
  }

  @Getter
  @NoArgsConstructor
  public static class Photo {
    private String name;
    private Integer widthPx;
    private Integer heightPx;
  }
}
