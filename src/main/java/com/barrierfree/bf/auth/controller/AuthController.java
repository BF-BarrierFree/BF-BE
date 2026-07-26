package com.barrierfree.bf.auth.controller;

import com.barrierfree.bf.auth.dto.AuthResponse;
import com.barrierfree.bf.auth.dto.KakaoLoginRequest;
import com.barrierfree.bf.auth.service.AuthService;
import com.barrierfree.bf.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 가입, 로그인, 토큰 재발급 등 인증/인가 관련 API를 처리하는 컨트롤러
 */
@Slf4j
@Tag(name = "인증 API", description = "로그인 및 로그아웃 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 소셜 로그인 API
     * 프론트엔드에서 발급받은 인가 코드(Authorization Code)를 받아 토큰과 유저 정보를 반환합니다.
     */
    @Operation(summary = "카카오 소셜 로그인", description = "카카오 인가 코드를 받아 유저를 생성하거나 로그인시키고, JWT 토큰을 반환합니다.")
    @PostMapping("/kakao/login")
    public ApiResponse<AuthResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        log.info("카카오 로그인 요청 수신");

        AuthResponse response = authService.kakaoLogin(request.code());

        // 프로젝트 컨벤션에 따른 일관된 응답 객체 반환
        return ApiResponse.success(response, "카카오 로그인이 완료되었습니다.");
    }

    @Operation(summary = "로그아웃", description = "현재 로그인한 유저의 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ApiResponse<?> logout(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        authService.logout(userId);
        return ApiResponse.successWithNoContent();
    }
}
