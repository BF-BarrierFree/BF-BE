package com.barrierfree.bf.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 API로부터 받아오는 유저 프로필 정보 DTO
 * 필요한 정보(소셜 고유 ID, 닉네임, 프로필 사진)만 매핑합니다.
 */
public record KakaoUserInfoResponse(
    @JsonProperty("id")
    Long id,
    
    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
        @JsonProperty("profile")
        Profile profile
    ) {}

    public record Profile(
        @JsonProperty("nickname")
        String nickname,
        
        @JsonProperty("profile_image_url")
        String profileImageUrl,
        
        @JsonProperty("is_default_image")
        Boolean isDefaultImage
    ) {}
}