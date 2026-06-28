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

  /** [테스트용] 공공데이터포털(ODsay) 연동 테스트: 대중교통 길찾기 응답 구조(경유하는 정류장, 위경도 등)를 눈으로 확인하기 위해 String으로 반환합니다. */
  public String testOdsayTransitRoute(
      double startLng, double startLat, double endLng, double endLat) {
    log.info(
        "ODsay 대중교통 길찾기 연동 테스트 시작 - 출발지: {},{}, 도착지: {},{}", startLng, startLat, endLng, endLat);

    // ODsay 대중교통 길찾기 API (경로탐색)
    // 파라미터: apiKey, SX(출발지 경도), SY(출발지 위도), EX(도착지 경도), EY(도착지 위도)
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

    String rawResponse =
        webClient
            .get()
            .uri(requestUrl)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("ODsay 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.ODSAY_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("ODsay 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.ODSAY_API_FAILED));
                })
            .bodyToMono(String.class)
            .block();

    if (rawResponse == null) {
      log.error("ODsay 응답이 null 입니다.");
      throw new CustomException(ErrorCode.ODSAY_API_FAILED);
    }

    return rawResponse;
  }
}
