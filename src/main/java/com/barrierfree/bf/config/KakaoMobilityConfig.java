package com.barrierfree.bf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoMobilityConfig {

    @Value("${kakao.mobility.base-url}")
    private String baseUrl;

    @Value("${kakao.mobility.api-key}")
    private String apiKey;

    @Bean
    public WebClient kakaoMobilityWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                // 카카오 모빌리티 API는 헤더에 KakaoAK {REST_API_KEY} 형식으로 인증
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}