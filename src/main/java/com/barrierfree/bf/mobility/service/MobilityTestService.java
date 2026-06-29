package com.barrierfree.bf.mobility.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.mobility.dto.CenterInfoResponse;
import com.barrierfree.bf.mobility.dto.VehicleInfoResponse;
import com.barrierfree.bf.mobility.dto.VehicleOperationResponse;
import com.barrierfree.bf.mobility.dto.VehicleUseResponse;
import com.barrierfree.bf.mobility.dto.external.CenterInfoRawResponse;
import com.barrierfree.bf.mobility.dto.external.VehicleInfoRawResponse;
import com.barrierfree.bf.mobility.dto.external.VehicleOperationRawResponse;
import com.barrierfree.bf.mobility.dto.external.VehicleUseRawResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class MobilityTestService {

    private final WebClient tagoWebClient;
    private final String baseUrl;
    private final String serviceKey;

    /**
     * yaml에 분리된 base-url과 공공데이터포털 공통 인증키를 함께 주입받습니다.
     */
    public MobilityTestService(
        @Qualifier("tagoWebClient") WebClient tagoWebClient,
        @Value("${mobility.api.base-url}") String baseUrl,
        @Value("${mobility.api.service-key}") String serviceKey
    ) {
        // 전국 단위 대용량 JSON 처리를 위해 WebClient의 버퍼 제한을 기본 256KB에서 10MB로 늘립니다.
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

        this.tagoWebClient = tagoWebClient.mutate()
            .exchangeStrategies(strategies)
            .build();
            
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    /**
     * [Step 1 테스트용] 특정 지자체의 교통약자 차량 실시간 운행 이력을 조회합니다.
     *
     * @param stdgCd 지자체 법정동코드 (예: 충북 청주시 "4311000000")
     * @param fromDate 조회 시작일 (YYYYMMDD)
     * @param toDate 조회 종료일 (YYYYMMDD)
     */
    public List<VehicleOperationResponse> getVehicleOperationHistory(String stdgCd, String fromDate, String toDate) {
        log.info("[MobilityTestService] 차량 운행이력 외부 API 호출 시작. 지자체코드: {}", stdgCd);

        // WebClientConfig에 EncodingMode.NONE이 설정되어 있으므로, 
        // UriBuilder를 쓰지 않고 명시적으로 URL 문자열을 조립하여 넘기는 것이 인증키 인코딩 에러를 막는 가장 안전한 방법입니다.
        String url = String.format("%s/info_vehicle_operation_v2?serviceKey=%s&pageNo=1&numOfRows=10&type=json&stdgCd=%s&fromCrtrYmd=%s&toCrtrYmd=%s",
            baseUrl, serviceKey, stdgCd, fromDate, toDate);

        VehicleOperationRawResponse rawResponse = tagoWebClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(VehicleOperationRawResponse.class)
            .block(); // 테스트용 블로킹

        if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
            log.error("[MobilityTestService] 공공데이터 API 응답 본문이 비어있습니다.");
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        VehicleOperationRawResponse.Header header = rawResponse.getHeaderSafe();
        String resultCode = header.getResultCode();
        
        // 공공데이터포털 표준 성공 코드는 "00"이나, 해당 행안부 API는 "K0" (또는 "200")을 성공으로 반환하므로 예외 처리
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 외부 API 에러 발생. Code: {}, Msg: {}", 
                resultCode, header.getResultMsg());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (rawResponse.getBodySafe() == null || rawResponse.getBodySafe().getItem() == null) {
            log.warn("[MobilityTestService] 조회된 차량 운행이력 데이터가 없습니다. 지자체: {}", stdgCd);
            return Collections.emptyList();
        }

        return rawResponse.getBodySafe().getItem().stream()
            .map(VehicleOperationResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * [Step 2 테스트용] 특정 지자체의 센터 상세 현황 정보(요금, 운영시간 등)를 조회합니다.
     */
    public List<CenterInfoResponse> getCenterInfo(String stdgCd) {
        log.info("[MobilityTestService] 센터 상세 현황 외부 API 호출 시작. 지자체코드: {}", stdgCd);

        String url = String.format("%s/center_info_v2?serviceKey=%s&pageNo=1&numOfRows=50&type=json&stdgCd=%s",
            baseUrl, serviceKey, stdgCd);

        CenterInfoRawResponse rawResponse = tagoWebClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(CenterInfoRawResponse.class)
            .block();

        if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String resultCode = rawResponse.getHeaderSafe().getResultCode();
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 센터 현황 API 에러 발생. Code: {}", resultCode);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (rawResponse.getBodySafe() == null || rawResponse.getBodySafe().getItem() == null) {
            return Collections.emptyList();
        }

        return rawResponse.getBodySafe().getItem().stream()
            .map(CenterInfoResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * [Step 3 테스트용] 전국 지자체의 센터 상세 현황 정보를 모두 조회합니다.
     * stdgCd(지자체코드) 파라미터를 생략하면 전국 데이터를 리턴합니다.
     */
    public List<CenterInfoResponse> getAllCenterInfo() {
        log.info("[MobilityTestService] 전국 센터 현황 외부 API 전체 호출 시작");

        // stdgCd 파라미터를 빼고, numOfRows를 500(전국 지자체 커버 가능하게 충분히 크게)으로 잡습니다.
        String url = String.format("%s/center_info_v2?serviceKey=%s&pageNo=1&numOfRows=500&type=json",
            baseUrl, serviceKey);

        CenterInfoRawResponse rawResponse = tagoWebClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(CenterInfoRawResponse.class)
            .block();

        if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String resultCode = rawResponse.getHeaderSafe().getResultCode();
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 전국 센터 현황 API 에러 발생. Code: {}", resultCode);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (rawResponse.getBodySafe() == null || rawResponse.getBodySafe().getItem() == null) {
            return Collections.emptyList();
        }

        return rawResponse.getBodySafe().getItem().stream()
            .map(CenterInfoResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * [Step 2 테스트용] 특정 지자체 차량들의 실시간 이용 및 대기인원 현황을 조회합니다.
     */
    public List<VehicleUseResponse> getVehicleUseInfo(String stdgCd) {
        log.info("[MobilityTestService] 실시간 이용 및 대기 현황 외부 API 호출 시작. 지자체코드: {}", stdgCd);

        String url = String.format("%s/info_vehicle_use_v2?serviceKey=%s&pageNo=1&numOfRows=50&type=json&stdgCd=%s",
            baseUrl, serviceKey, stdgCd);

        VehicleUseRawResponse rawResponse = tagoWebClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(VehicleUseRawResponse.class)
            .block();

        if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String resultCode = rawResponse.getHeaderSafe().getResultCode();
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 실시간 이용현황 API 에러 발생. Code: {}", resultCode);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (rawResponse.getBodySafe() == null || rawResponse.getBodySafe().getItem() == null) {
            return Collections.emptyList();
        }

        return rawResponse.getBodySafe().getItem().stream()
            .map(VehicleUseResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * [Step 2 테스트용] 특정 지자체 보유 차량들의 제원 정보(휠체어석 수 등)를 조회합니다.
     */
    public List<VehicleInfoResponse> getVehicleInfo(String stdgCd) {
        log.info("[MobilityTestService] 차량 기본 스펙 외부 API 호출 시작. 지자체코드: {}", stdgCd);

        String url = String.format("%s/info_vehicle_v2?serviceKey=%s&pageNo=1&numOfRows=100&type=json&stdgCd=%s",
            baseUrl, serviceKey, stdgCd);

        VehicleInfoRawResponse rawResponse = tagoWebClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(VehicleInfoRawResponse.class)
            .block();

        if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String resultCode = rawResponse.getHeaderSafe().getResultCode();
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 차량 스펙 API 에러 발생. Code: {}", resultCode);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (rawResponse.getBodySafe() == null || rawResponse.getBodySafe().getItem() == null) {
            return Collections.emptyList();
        }

        return rawResponse.getBodySafe().getItem().stream()
            .map(VehicleInfoResponse::from)
            .collect(Collectors.toList());
    }
}