package com.barrierfree.bf.auth.dto;

import com.barrierfree.bf.global.enums.Role;
import lombok.Builder;

/** 프론트엔드로 반환하는 최종 로그인 응답 DTO */
@Builder
public record AuthResponse(
    String accessToken,
    String refreshToken,
    Role role, // GUEST면 온보딩 페이지로, USER면 메인 페이지로 유도
    boolean isNewUser // 최초 가입자인지 여부 (환영 팝업 등 프론트엔드 분기용)
    ) {}
