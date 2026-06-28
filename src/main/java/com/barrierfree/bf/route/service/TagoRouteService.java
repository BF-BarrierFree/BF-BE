package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TagoRouteService {

  private final WebClient tagoWebClient;

  @Value("${tago.api.base-url}")
  private String baseUrl;

  @Value("${tago.api.station-base-url}")
  private String stationBaseUrl;

  @Value("${tago.api.service-key}")
  private String serviceKey;

  // 공공데이터포털(TAGO) 전용으로 만든 tagoWebClient 빈을 명시적으로 주입
  public TagoRouteService(@Qualifier("tagoWebClient") WebClient tagoWebClient) {
    this.tagoWebClient = tagoWebClient;
  }

  /**
   * [테스트용 1] 공공데이터포털(TAGO) 연동 테스트: 도시코드 목록 조회 연결 및 인증 성공 여부를 직관적으로 확인하기 위해 원본 String 형태로 데이터를
   * 반환합니다.
   */
  public String testTagoCityCodeConnection() {
    log.info("TAGO 도시코드 목록 조회 연동 테스트 시작");

    String requestUrl = baseUrl + "/getCtyCodeList?serviceKey=" + serviceKey + "&_type=json";

    String rawResponse =
        tagoWebClient
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
                              log.error("TAGO 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .block();

    if (rawResponse == null) {
      log.error("TAGO 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }

  /**
   * [테스트용 2] 공공데이터포털(TAGO) 연동 테스트: 정류소별 버스 도착예정정보 응답 데이터 중 'vehicletp' 필드가 '저상버스'인지 확인하기 위한 테스트입니다.
   */
  public String testTagoBusArrivalInfo(int cityCode, String nodeId) {
    log.info("TAGO 버스 도착예정정보(저상버스) 연동 테스트 시작 - cityCode: {}, nodeId: {}", cityCode, nodeId);

    String requestUrl =
        baseUrl
            + "/getSttnAcctoArvlPrearngeInfoList"
            + "?serviceKey="
            + serviceKey
            + "&cityCode="
            + cityCode
            + "&nodeId="
            + nodeId
            + "&numOfRows=10"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient
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
                              log.error("TAGO 버스 도착정보 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 버스 도착정보 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .block();

    if (rawResponse == null) {
      log.error("TAGO 버스 도착정보 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }

  /**
   * [테스트용 3] 공공데이터포털(TAGO) 연동 테스트: 좌표기반 근접 정류소 목록 조회 ODsay에서 얻은 위경도를 통해 TAGO의 정류소 ID(nodeId)와
   * 도시코드(cityCode)를 알아냅니다.
   */
  public String testTagoNearbyStation(double lat, double lng) {
    log.info("TAGO 근접 정류소 조회 연동 테스트 시작 - lat: {}, lng: {}", lat, lng);

    String requestUrl =
        stationBaseUrl
            + "/getCrdntPrxmtSttnList"
            + "?serviceKey="
            + serviceKey
            + "&gpsLati="
            + lat
            + "&gpsLong="
            + lng
            + "&numOfRows=10"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient
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
                              log.error("TAGO 근접 정류소 조회 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 근접 정류소 조회 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .block();

    if (rawResponse == null) {
      log.error("TAGO 근접 정류소 조회 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }
}
