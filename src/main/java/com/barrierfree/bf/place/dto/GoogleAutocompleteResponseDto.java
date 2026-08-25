package com.barrierfree.bf.place.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구글 Places API (New) - Autocomplete(자동완성) 응답을 매핑하기 위한 DTO */
@Getter
@NoArgsConstructor
public class GoogleAutocompleteResponseDto {

  private List<Suggestion> suggestions;

  @Getter
  @NoArgsConstructor
  public static class Suggestion {
    private PlacePrediction placePrediction;
  }

  @Getter
  @NoArgsConstructor
  public static class PlacePrediction {
    // 제안된 장소의 고유 리소스 이름 (예: places/ChIJ...)
    private String place;
    // 제안된 장소의 고유 ID (상세 조회용으로 쓰임)
    private String placeId;
    // 화면에 보여줄 추천 텍스트 전체 문자열
    private FormattedText text;
    // 주요 텍스트와 보조 텍스트가 분리된 구조화된 정보
    private StructuredFormat structuredFormat;
    // 장소 카테고리 타입 배열 (예: ["cafe", "food"])
    private List<String> types;
  }

  @Getter
  @NoArgsConstructor
  public static class StructuredFormat {
    private FormattedText mainText; // 주요 텍스트 (예: "스타벅스 홍대점")
    private FormattedText secondaryText; // 보조 텍스트 (예: "대한민국 서울특별시 마포구...")
  }

  @Getter
  @NoArgsConstructor
  public static class FormattedText {
    private String text; // 실제 노출되는 문자열
  }
}
