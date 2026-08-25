package com.barrierfree.bf.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 인증 서버로부터 받는 Access Token 응답 DTO 불변성을 보장하기 위해 Java record 활용 */
public record KakaoTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("refresh_token_expires_in") Integer refreshTokenExpiresIn) {}
