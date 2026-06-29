package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMobilityService {

    private final WebClient kakaoMobilityWebClient;

    /**
     * [테스트용] 카카오 내비 실시간 길찾기 API 호출
     */
    public String testKakaoDirections(
            String origin,
            String destination,
            String priority, // 단일 우선순위 옵션
            Boolean alternatives,
            List<String> avoid,
            Integer roadevent) { // 교통 장애(유고) 반영 옵션

        log.debug(
                "카카오 실시간 길찾기 테스트 - 출발지: {}, 도착지: {}, priority: {}, roadevent: {}",
                origin,
                destination,
                priority,
                roadevent);

        String priorityParam = (priority != null && !priority.trim().isEmpty()) ? priority.trim() : "RECOMMEND";
        String avoidParam = formatAvoid(avoid); // 콤마 대신 파이프(|)로 조인

        return kakaoMobilityWebClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder
                                    .path("/v1/directions")
                                    .queryParam("origin", origin)
                                    .queryParam("destination", destination)
                                    .queryParam("priority", priorityParam)
                                    .queryParam("roadevent", roadevent != null ? roadevent : 0)
                                    .queryParam("car_fuel", "LPG")
                                    .queryParam("car_hipass", true);

                            if (alternatives != null) {
                                uriBuilder.queryParam("alternatives", alternatives);
                            }
                            if (avoidParam != null) {
                                uriBuilder.queryParam("avoid", avoidParam);
                            }
                            return uriBuilder.build();
                        })
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                errorBody -> {
                                                    log.error("카카오 실시간 길찾기 4xx 에러 발생: {}", errorBody);
                                                    return Mono.error(new CustomException(ErrorCode.KAKAO_API_FAILED));
                                                }))
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                errorBody -> {
                                                    log.error("카카오 실시간 길찾기 5xx 에러 발생: {}", errorBody);
                                                    return Mono.error(new CustomException(ErrorCode.KAKAO_API_FAILED));
                                                }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(
                        e -> {
                            if (!(e instanceof CustomException)) {
                                log.error("카카오 실시간 길찾기 API 네트워크/타임아웃 에러: {}", e.getMessage());
                                return new CustomException(ErrorCode.KAKAO_API_TIMEOUT);
                            }
                            return e;
                        })
                .block();
    }

    /**
     * [테스트용] 카카오 내비 미래 운행 정보 길찾기 API 호출
     * 과거 교통량 패턴을 기반으로 미래의 특정 출발 시간에 대한 경로 및 ETA를 예측합니다.
     */
    public String testKakaoFutureDirections(
            String origin,
            String destination,
            String departureTime,
            String priority, // 단일 우선순위 옵션
            Boolean alternatives,
            List<String> avoid,
            Integer roadevent) { // 교통 장애(유고) 반영 옵션

        log.debug(
                "카카오 미래 운행 길찾기 테스트 - 출발지: {}, 도착지: {}, 출발시간: {}, priority: {}, roadevent: {}",
                origin,
                destination,
                departureTime,
                priority,
                roadevent);

        String priorityParam = (priority != null && !priority.trim().isEmpty()) ? priority.trim() : "RECOMMEND";
        String avoidParam = formatAvoid(avoid); // 콤마 대신 파이프(|)로 조인

        return kakaoMobilityWebClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder
                                    .path("/v1/future/directions") // 미래 운행 정보 전용 엔드포인트
                                    .queryParam("origin", origin)
                                    .queryParam("destination", destination)
                                    .queryParam("departure_time", departureTime) // 필수 파라미터 추가
                                    .queryParam("priority", priorityParam)
                                    .queryParam("roadevent", roadevent != null ? roadevent : 0)
                                    .queryParam("car_fuel", "LPG")
                                    .queryParam("car_hipass", true);

                            if (alternatives != null) {
                                uriBuilder.queryParam("alternatives", alternatives);
                            }
                            if (avoidParam != null) {
                                uriBuilder.queryParam("avoid", avoidParam);
                            }
                            return uriBuilder.build();
                        })
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                errorBody -> {
                                                    log.error("카카오 미래 운행 길찾기 4xx 에러 발생: {}", errorBody);
                                                    return Mono.error(new CustomException(ErrorCode.KAKAO_API_FAILED));
                                                }))
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                errorBody -> {
                                                    log.error("카카오 미래 운행 길찾기 5xx 에러 발생: {}", errorBody);
                                                    return Mono.error(new CustomException(ErrorCode.KAKAO_API_FAILED));
                                                }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(
                        e -> {
                            if (!(e instanceof CustomException)) {
                                log.error("카카오 미래 운행 길찾기 API 네트워크/타임아웃 에러: {}", e.getMessage());
                                return new CustomException(ErrorCode.KAKAO_API_TIMEOUT);
                            }
                            return e;
                        })
                .block();
    }

    /**
     * [내부 로직] 회피 옵션 포맷팅
     * 카카오 API 스펙에 맞게 다중 회피 옵션을 파이프(|)로 연결하여 반환합니다.
     */
    private String formatAvoid(List<String> avoid) {
        if (avoid == null || avoid.isEmpty()) {
            return null;
        }
        String result = avoid.stream()
                .map(String::trim)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining("|")); // 파이프(|) 연산자로 결합
        return result.isEmpty() ? null : result;
    }
}