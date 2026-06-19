package com.barrierfree.bf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  /** 비동기 HTTP 통신을 위한 WebClient 빈(Bean) 등록 서비스 계층(Service)에서 주입받아 구글 API 등을 호출할 때 사용합니다. */
  @Bean
  public WebClient webClient() {
    return WebClient.builder()
        // 필요한 경우 여기서 기본 타임아웃, 로깅 필터 등을 추가할 수 있습니다.
        // 지금은 가장 기본적인 형태의 WebClient를 생성합니다.
        .build();
  }
}
