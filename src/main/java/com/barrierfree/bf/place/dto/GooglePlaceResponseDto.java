package com.barrierfree.bf.place.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구글 Places API (New) 응답을 매핑하기 위한 최종 DTO 장소 기본 정보, 무장애(접근성) 데이터, 영업/연락처 정보, 요약/사진 등을 모두 포함합니다. */
@Getter
@NoArgsConstructor
public class GooglePlaceResponseDto {

  private List<Place> places;

  @Getter
  @NoArgsConstructor
  public static class Place {
    private String id;
    private LocalizedText displayName; // 장소명
    private String formattedAddress; // 주소
    private Location location; // 위도, 경도

    // --- 1. 리뷰 및 요약 ---
    private List<Review> reviews; // 최대 5개의 리뷰
    private LocalizedText editorialSummary; // 구글 에디터 한 줄 요약
    private GenerativeSummary generativeSummary; // AI 기반 리뷰/분위기 요약

    // --- 2. 무장애(접근성) 특화 필드 (객체로 묶여서 내려옴) ---
    private AccessibilityOptions accessibilityOptions;

    // --- 3. 부가 정보 (영업, 연락처, 부대시설 등) ---
    private String nationalPhoneNumber; // 국내 형식 전화번호 (예: 02-123-4567)
    private String websiteUri; // 공식 홈페이지 주소
    private Boolean allowsDogs; // 반려견 동반 가능 여부 (참고용)
    private ParkingOptions parkingOptions; // 주차장 상세 옵션
    private OpeningHours regularOpeningHours; // 일반 영업시간
    private List<Photo> photos; // 장소 사진 메타데이터 (프론트에서 이미지 호출용)
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

  // --- 구조화된 객체 클래스들 ---

  @Getter
  @NoArgsConstructor
  public static class AccessibilityOptions {
    private Boolean wheelchairAccessibleParking; // 휠체어 전용 주차장 여부
    private Boolean wheelchairAccessibleEntrance; // 휠체어 접근 가능 입구 여부
    private Boolean wheelchairAccessibleRestroom; // 휠체어 접근 가능 화장실 여부
    private Boolean wheelchairAccessibleSeating; // 휠체어 접근 가능 좌석 여부
  }

  @Getter
  @NoArgsConstructor
  public static class ParkingOptions {
    private Boolean freeParkingLot; // 무료 주차장 여부
    private Boolean paidParkingLot; // 유료 주차장 여부
    private Boolean valetParking; // 발렛 파킹 여부
  }

  @Getter
  @NoArgsConstructor
  public static class OpeningHours {
    private Boolean openNow; // 현재 영업 중인지 여부
    private List<String> weekdayDescriptions; // 월~일 요일별 텍스트 영업시간
  }

  @Getter
  @NoArgsConstructor
  public static class Photo {
    private String name; // 사진 리소스 이름 (ex: places/123/photos/456)
    private Integer widthPx;
    private Integer heightPx;
  }
}
