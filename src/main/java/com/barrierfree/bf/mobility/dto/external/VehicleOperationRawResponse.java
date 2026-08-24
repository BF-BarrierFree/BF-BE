package com.barrierfree.bf.mobility.dto.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 행정안전부 교통약자 이동지원 차량 실시간 운행이력 API 응답 Raw DTO
 * * [실무 방어 로직 안내]
 * 1. 공공데이터포털 JSON 응답은 최상위에 "response" 객체가 감싸져 오는 규격과, 
 * 그렇지 않은 규격이 서버마다 혼재하므로 두 규격을 모두 안전하게 파싱하도록 Wrapper를 설계했습니다.
 * 2. 조회 결과가 딱 1건일 때 Json Array([ ])가 아닌 Json Object({ })로 내려오는 
 * 공공데이터포털 고질적 버그를 방지하기 위해 ACCEPT_SINGLE_VALUE_AS_ARRAY 옵션을 적용했습니다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleOperationRawResponse {

    private Header header;
    private Body body;
    private ResponseWrapper response;

    public Header getHeaderSafe() {
        if (this.response != null && this.response.getHeader() != null) {
            return this.response.getHeader();
        }
        return this.header;
    }

    public Body getBodySafe() {
        if (this.response != null && this.response.getBody() != null) {
            return this.response.getBody();
        }
        return this.body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseWrapper {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String numOfRows;
        private String pageNo;
        private String totalCount;

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String operSttsYn;
        private String totDt;
        private String stdgCd;
        private String lclgvNm;
        private String cntrId;
        private String cntrNm;
        private String vhclId;
        private String vhclUsgNm;
        private String vhclMdlNm;
    }
}