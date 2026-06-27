package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.dto.OrsGeoJsonResponse;
import com.barrierfree.bf.route.dto.OrsRouteRequest;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrsRouteService {

  private final WebClient webClient;

  @Value("${openrouteservice.base-url}")
  private String baseUrl;

  @Value("${openrouteservice.api-key}")
  private String apiKey;

  /**
   * 외부 ORS 서버를 호출하여 휠체어 전용 경로(계단 회피 등)를 반환합니다. 동일한 좌표로 반복 요청이 들어올 경우, 서버 외부로 나가지 않고 캐시에서 즉시 응답을
   * 반환합니다.
   */
  @Cacheable(
      value = "ors_routes",
      key = "#startLng + '_' + #startLat + '_' + #endLng + '_' + #endLat")
  public WheelchairRouteResponse getWheelchairRoute(
      double startLng, double startLat, double endLng, double endLat) {

    // 1. 요청 페이로드 생성 (계단 회피 옵션 포함)
    OrsRouteRequest requestPayload =
        OrsRouteRequest.createWheelchair(startLng, startLat, endLng, endLat);

    // 2. ORS API 비동기 호출 및 에러 핸들링
    OrsGeoJsonResponse rawResponse =
        webClient
            .mutate() // 기존 빈 객체를 복제하여 설정 덮어쓰기 방지
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri("/v2/directions/wheelchair/geojson")
            .header(HttpHeaders.AUTHORIZATION, apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestPayload)
            .retrieve()
            // 4xx 에러: 좌표가 잘못되었거나, 경로를 찾을 수 없는 경우
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("ORS 클라이언트 에러(4xx) 발생. Response: {}", errorBody);

                              // ORS에서 흔하게 발생하는 에러 코드 파싱 (필요에 따라 분기 처리 고도화 가능)
                              if (errorBody.contains("\"code\":2009")
                                  || errorBody.contains("\"code\":2010")
                                  || errorBody.contains("could not be found")) {
                                return Mono.error(new CustomException(ErrorCode.ROUTE_NOT_FOUND));
                              } else if (errorBody.contains("Invalid coordinates")) {
                                return Mono.error(
                                    new CustomException(ErrorCode.INVALID_ROUTE_COORDINATES));
                              }

                              // 알 수 없는 4xx 에러일 경우
                              return Mono.error(new CustomException(ErrorCode.INVALID_INPUT_VALUE));
                            }))
            // 5xx 에러: ORS 서버 자체가 터졌거나 한도 초과 등 외부 이슈
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("ORS 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.EXTERNAL_ROUTING_FAILED));
                })
            .bodyToMono(OrsGeoJsonResponse.class)
            .block(); // Service 계층에서는 동기적으로 차단(blocking)하여 결과 반환

    // 3. 정상 응답이지만 좌표 데이터가 비어있는 예외 상황 처리
    if (rawResponse == null || rawResponse.features() == null || rawResponse.features().isEmpty()) {
      log.error("ORS 응답 성공했으나 Features가 비어있음.");
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    // 4. 무거운 원본 응답을 FE에서 사용하기 좋게 가벼운 DTO로 변환하여 반환
    return WheelchairRouteResponse.from(rawResponse);
  }
}
