package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdsayRouteService {

    private final WebClient webClient; // 일반 WebClient 사용 (Config에서 만든 기본 Bean)

    @Value("${odsay.api.base-url}")
    private String baseUrl;

    @Value("${odsay.api.api-key}")
    private String apiKey;

    /**
     * [테스트용] 공공데이터포털(ODsay) 연동 테스트
     * 1번(출발시간), 4번(교통수단 분리), 7번(최적경로/정렬옵션) 검증을 위해 파라미터 확장
     */
    public String testOdsayTransitRoute(
            double startLng,
            double startLat,
            double endLng,
            double endLat,
            Integer searchPathType,
            String time,
            Integer opt) {

        log.info(
                "ODsay 대중교통 길찾기 연동 테스트 시작 - 출발지: {},{}, 도착지: {},{}, Type: {}, Time: {}, OPT: {}",
                startLng, startLat, endLng, endLat, searchPathType, time, opt);

        // 기본 필수 파라미터 셋팅
        String requestUrl =
                baseUrl
                        + "/v1/api/searchPubTransPathT"
                        + "?apiKey="
                        + apiKey
                        + "&SX="
                        + startLng
                        + "&SY="
                        + startLat
                        + "&EX="
                        + endLng
                        + "&EY="
                        + endLat;

        // [추가] 4. 버스/지하철 분리 파라미터 (0: 모두, 1: 지하철, 2: 버스)
        if (searchPathType != null) {
            requestUrl += "&SearchPathType=" + searchPathType;
        }

        // [추가] 1. 출발 시간 지정 파라미터 (ODsay 스펙상 시간 지정이 가능한지 테스트하기 위함)
        if (time != null && !time.isEmpty()) {
            requestUrl += "&time=" + time;
        }

        // [추가] 7. 경로 정렬 옵션 (0: 최적, 1: 최단시간, 2: 최소환승, 3: 최소도보)
        if (opt != null) {
            requestUrl += "&OPT=" + opt;
        }

        String rawResponse =
                webClient
                        .get()
                        .uri(requestUrl)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::is4xxClientError,
                                response ->
                                        response.bodyToMono(String.class)
                                                .flatMap(
                                                        errorBody -> {
                                                            log.error(
                                                                    "ODsay 클라이언트 에러(4xx) 발생. Response: {}",
                                                                    errorBody);
                                                            return Mono.error(
                                                                    new CustomException(
                                                                            ErrorCode.ODSAY_API_FAILED));
                                                        }))
                        .onStatus(
                                HttpStatusCode::is5xxServerError,
                                response -> {
                                    log.error("ODsay 외부 서버 에러(5xx) 발생.");
                                    return Mono.error(new CustomException(ErrorCode.ODSAY_API_FAILED));
                                })
                        .bodyToMono(String.class)
                        .onErrorMap(
                                throwable -> {
                                    log.error(
                                            "ODsay API 호출 중 네트워크/타임아웃 에러 발생: {}",
                                            throwable.getMessage());
                                    return new CustomException(ErrorCode.ODSAY_API_FAILED);
                                })
                        .block();

        if (rawResponse == null) {
            log.error("ODsay 응답이 null 입니다.");
            throw new CustomException(ErrorCode.ODSAY_API_FAILED);
        }

        return rawResponse;
    }
}