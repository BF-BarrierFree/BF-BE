package com.barrierfree.bf.auth.client;

import com.barrierfree.bf.auth.dto.KakaoTokenResponse;
import com.barrierfree.bf.auth.dto.KakaoUserInfoResponse;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** 카카오 API 서버와의 실제 HTTP 통신을 담당하는 외부 클라이언트 컴포넌트 */
@Component
public class KakaoOAuthClient {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private final String userInfoUri;
    // 💡 기존에 있던 필드 멤버 redirectUri 삭제

    // WebClient.Builder 대신 이미 Bean으로 등록된 WebClient를 바로 주입받습니다.
    public KakaoOAuthClient(
        WebClient webClient,
        @Value("${oauth2.kakao.client-id}") String clientId,
        @Value("${oauth2.kakao.client-secret}") String clientSecret,
        // 💡 기존에 있던 @Value("${oauth2.kakao.redirect-uri}") 주입 삭제
        @Value("${oauth2.kakao.token-uri}") String tokenUri,
        @Value("${oauth2.kakao.user-info-uri}") String userInfoUri) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    /** 프론트엔드에서 전달받은 인가 코드(Authorization Code)와 리다이렉트 URI로 카카오 Access Token을 요청합니다. */
    public String getAccessToken(String authorizationCode, String redirectUri) { // 💡 파라미터에 redirectUri 추가
        KakaoTokenResponse tokenResponse =
            webClient
                .post()
                .uri(tokenUri)
                .header(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=utf-8")
                .body(
                    BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri) // 💡 파라미터로 받은 동적 URI 사용
                        .with("code", authorizationCode)
                        .with("client_secret", clientSecret))
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError(),
                    clientResponse -> Mono.error(new CustomException(ErrorCode.KAKAO_LOGIN_FAILED)))
                .onStatus(
                    status -> status.is5xxServerError(),
                    clientResponse ->
                        Mono.error(new CustomException(ErrorCode.KAKAO_SERVICE_UNAVAILABLE)))
                .bodyToMono(KakaoTokenResponse.class)
                .block(); // 외부 연동이므로 결과값을 기다립니다.

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        return tokenResponse.accessToken();
    }

    /** 카카오 Access Token을 사용해 사용자 프로필 정보를 조회합니다. */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        KakaoUserInfoResponse userInfo =
            webClient
                .get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=utf-8")
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError(),
                    clientResponse -> Mono.error(new CustomException(ErrorCode.KAKAO_USER_INFO_FAILED)))
                .onStatus(
                    status -> status.is5xxServerError(),
                    clientResponse ->
                        Mono.error(new CustomException(ErrorCode.KAKAO_SERVICE_UNAVAILABLE)))
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();

        if (userInfo == null) {
            throw new CustomException(ErrorCode.KAKAO_USER_INFO_FAILED);
        }

        return userInfo;
    }
}
