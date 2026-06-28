package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMobilityService {

    private final WebClient kakaoMobilityWebClient;

    /**
     * [테스트용] 카카오 내비 길찾기 API 호출
     * origin, destination 좌표 형식: "경도,위도" (예: 127.1101,37.3947)
     */
    public String testKakaoDirections(String origin, String destination) {
        log.info("카카오 모빌리티 길찾기 테스트 시작 - 출발지: {}, 도착지: {}", origin, destination);

        return kakaoMobilityWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(e -> {
                    log.error("카카오 API 호출 실패: {}", e.getMessage());
                    return new CustomException(ErrorCode.ODSAY_API_FAILED); // 추후 KAKAO_API_FAILED로 별도 분리
                })
                .block();
    }
}