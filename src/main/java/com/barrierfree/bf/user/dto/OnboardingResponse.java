package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.global.enums.Role;
import lombok.Builder;
import lombok.Getter;

/**
 * 온보딩 성공 후 프론트엔드에 반환할 DTO
 * USER로 승격된 권한이 담긴 새로운 AccessToken과 RefreshToken을 전달합니다.
 */
@Getter
@Builder
public class OnboardingResponse {

    private String accessToken;
    private String refreshToken;
    private String nickname;
    private Role role; // GUEST -> USER 승격 확인용
}
