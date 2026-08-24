package com.barrierfree.bf.taxi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공공데이터포털 전국특별교통수단(장애인콜택시)표준데이터 API 응답 DTO 불규칙한 JSON 응답(item이 1개일 때 객체, 여러 개일 때 배열)을 유연하게 파싱합니다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagoTaxiCenterResponse {

  private Response response;
  private Body body; // API에 따라 최상단에 바로 body가 오는 경우도 대비

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Response {
    private Body body;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Body {
    private String totalCount;
    private String pageNo;
    private String numOfRows;

    // 공공데이터 API는 반환값이 1개일 때 배열이 아닌 단일 객체로 반환하는 고질적 문제가 있어,
    // ACCEPT_SINGLE_VALUE_AS_ARRAY 설정을 활성화한 ObjectMapper를 사용하거나 아래와 같이 선언합니다.
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<CenterItem> item;
  }

  @Getter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CenterItem {
    private String cntrId;
    private String cntrNm;
    private String lclgvNm;
    private String cntrRoadNmAddr;
    private String lat;
    private String lot;
    private String cntrTelno;
    private String rsvtSiteUrlAddr;
    private String appSrvcNm;
    private String wkdyRsvtBgngTm;
    private String wkdyRsvtEndTm;
    private String wkdyOprBgngTm;
    private String wkdyOprEndTm;
    private String wkndOperYn;
    private String wkndOperHrExpln;
    private String wtjrOprRgnNm;
    private String btjrOprRgnNm;
    private String utztnTrgtExpln;
    private String dayVhclUtztnNmtm;
    private String bfhdRsvtPrdExpln;
    private String bscCrgExpln;
    private String exchrgCrgExpln;
    private String rsvtGdMttr;
    private String totCrtrYmd;
  }
}
