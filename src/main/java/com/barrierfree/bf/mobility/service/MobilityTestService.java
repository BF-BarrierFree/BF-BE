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
import java.util.ArrayList;
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

        VehicleOperationRawResponse rawResponse = tagoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/info_vehicle_operation_v2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("type", "json")
                .queryParam("stdgCd", stdgCd)
                .queryParam("fromCrtrYmd", fromDate)
                .queryParam("toCrtrYmd", toDate)
                .build())
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

        CenterInfoRawResponse rawResponse = tagoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/center_info_v2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 50)
                .queryParam("type", "json")
                .queryParam("stdgCd", stdgCd)
                .build())
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

        List<CenterInfoResponse> allItems = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 500;
        int totalCount = 0;

        // 첫 페이지를 조회하여 totalCount 확인
        CenterInfoRawResponse firstResponse = tagoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/center_info_v2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .build())
            .retrieve()
            .bodyToMono(CenterInfoRawResponse.class)
            .block();

        if (firstResponse == null || firstResponse.getHeaderSafe() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String resultCode = firstResponse.getHeaderSafe().getResultCode();
        if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
            log.error("[MobilityTestService] 전국 센터 현황 API 에러 발생. Code: {}", resultCode);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (firstResponse.getBodySafe() == null || firstResponse.getBodySafe().getItem() == null) {
            return Collections.emptyList();
        }

        // 첫 페이지 데이터 추가
        allItems.addAll(firstResponse.getBodySafe().getItem().stream()
            .map(CenterInfoResponse::from)
            .collect(Collectors.toList()));

        // totalCount 추출
        if (firstResponse.getBodySafe().getTotalCount() != null) {
            try {
                totalCount = Integer.parseInt(firstResponse.getBodySafe().getTotalCount());
                log.info("[MobilityTestService] 전국 센터 총 개수: {}", totalCount);
            } catch (NumberFormatException e) {
                log.warn("[MobilityTestService] totalCount 파싱 실패. 첫 페이지 결과만 반환합니다.");
                return allItems;
            }
        } else {
            log.warn("[MobilityTestService] totalCount 정보가 없습니다. 첫 페이지 결과만 반환합니다.");
            return allItems;
        }

        // 나머지 페이지 조회
        int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
        for (pageNo = 2; pageNo <= totalPages; pageNo++) {
            log.info("[MobilityTestService] {}페이지 조회 중...", pageNo);

            final int currentPage = pageNo;
            CenterInfoRawResponse rawResponse = tagoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/center_info_v2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", currentPage)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("type", "json")
                    .build())
                .retrieve()
                .bodyToMono(CenterInfoRawResponse.class)
                .block();

            if (rawResponse == null || rawResponse.getHeaderSafe() == null) {
                log.error("[MobilityTestService] {}페이지 조회 실패", currentPage);
                continue;
            }

            resultCode = rawResponse.getHeaderSafe().getResultCode();
            if (!"00".equals(resultCode) && !"K0".equals(resultCode) && !"200".equals(resultCode)) {
                log.error("[MobilityTestService] {}페이지 API 에러 발생. Code: {}", currentPage, resultCode);
                continue;
            }

            if (rawResponse.getBodySafe() != null && rawResponse.getBodySafe().getItem() != null) {
                allItems.addAll(rawResponse.getBodySafe().getItem().stream()
                    .map(CenterInfoResponse::from)
                    .collect(Collectors.toList()));
            }
        }

        log.info("[MobilityTestService] 전국 센터 현황 전체 조회 완료. 총 {}개", allItems.size());
        return allItems;
    }

    /**
     * [Step 2 테스트용] 특정 지자체 차량들의 실시간 이용 및 대기인원 현황을 조회합니다.
     */
    public List<VehicleUseResponse> getVehicleUseInfo(String stdgCd) {
        log.info("[MobilityTestService] 실시간 이용 및 대기 현황 외부 API 호출 시작. 지자체코드: {}", stdgCd);

        VehicleUseRawResponse rawResponse = tagoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/info_vehicle_use_v2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 50)
                .queryParam("type", "json")
                .queryParam("stdgCd", stdgCd)
                .build())
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

        VehicleInfoRawResponse rawResponse = tagoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/info_vehicle_v2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .queryParam("type", "json")
                .queryParam("stdgCd", stdgCd)
                .build())
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