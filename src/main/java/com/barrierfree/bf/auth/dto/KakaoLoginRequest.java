package com.barrierfree.bf.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 프론트엔드로부터 카카오 인가 코드와 리다이렉트 URI를 전달받기 위한 Request DTO */
public record KakaoLoginRequest(
    @NotBlank(message = "카카오 인가 코드는 필수입니다.")
    String code,

    @NotBlank(message = "리다이렉트 URI는 필수입니다.")
    String redirectUri
) {}
