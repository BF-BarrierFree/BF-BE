package com.barrierfree.bf.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  /** 비동기 HTTP 통신을 위한 WebClient 빈(Bean) 등록 서비스 계층(Service)에서 주입받아 구글 API 등을 호출할 때 사용합니다. */
  @Bean
  public WebClient webClient() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }

  /**
   * 공공데이터포털(TAGO) API 호출 전용 WebClient 빈(Bean). 공공데이터포털 인증키는 이미 인코딩된 상태로 발급되는 경우가 많아, WebClient가 요청을
   * 보낼 때 URL을 한 번 더 인코딩(Double Encoding)하는 것을 방지합니다.
   */
  @Bean
  public WebClient tagoWebClient() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
    // 공공데이터포털 고질적 에러(SERVICE_KEY_IS_NOT_REGISTERED_ERROR) 방지 설정
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

    return WebClient.builder()
        .uriBuilderFactory(factory)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
