package com.barrierfree.bf.mobility.dto.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleUseRawResponse {

    private Header header;
    private Body body;
    private ResponseWrapper response;

    public Header getHeaderSafe() {
        return (this.response != null && this.response.getHeader() != null) ? this.response.getHeader() : this.header;
    }

    public Body getBodySafe() {
        return (this.response != null && this.response.getBody() != null) ? this.response.getBody() : this.body;
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
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String totDt;
        private String cntrNm;
        private String tvhclCntom;
        private String oprVhclCntom;
        private String avlVhclCntom;
        private String wtngNocs;
    }
}