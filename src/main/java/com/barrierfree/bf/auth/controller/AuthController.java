package com.barrierfree.bf.auth.controller;

import com.barrierfree.bf.auth.dto.AuthResponse;
import com.barrierfree.bf.auth.dto.KakaoLoginRequest;
import com.barrierfree.bf.auth.service.AuthService;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 회원 가입, 로그인, 토큰 재발급 등 인증/인가 관련 API를 처리하는 컨트롤러 */
@Slf4j
@Tag(name = "인증 API", description = "로그인 및 로그아웃 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  // 테스트 토큰 발급을 위해 추가로 의존성을 주입합니다.
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;

  /** 카카오 소셜 로그인 API 프론트엔드에서 발급받은 인가 코드(Authorization Code)를 받아 토큰과 유저 정보를 반환합니다. */
  @Operation(summary = "카카오 소셜 로그인", description = "카카오 인가 코드를 받아 유저를 생성하거나 로그인시키고, JWT 토큰을 반환합니다.")
  @PostMapping("/kakao/login")
  public ApiResponse<AuthResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
    log.info("카카오 로그인 요청 수신 (code: {}, redirectUri: {})", request.code(), request.redirectUri());

    // code와 redirectUri를 함께 넘겨줍니다.
    AuthResponse response = authService.kakaoLogin(request.code(), request.redirectUri());

    return ApiResponse.success(response, "카카오 로그인이 완료되었습니다.");
  }

  @Operation(summary = "로그아웃", description = "현재 로그인한 유저의 Refresh Token을 삭제하여 로그아웃 처리합니다.")
  @PostMapping("/logout")
  public ApiResponse<?> logout(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
    authService.logout(userId);
    return ApiResponse.successWithNoContent();
  }

  @Operation(
      summary = "🚀 [개발용] 테스트 토큰 발급",
      description = "카카오 로그인 우회용. 버튼을 누르면 강제로 USER 권한 토큰을 발급합니다.")
  @GetMapping("/test-login")
  // @Profile("local") // 🚨 실무 팁: 운영(prod) 서버에 배포될 때 이 API가 열리는 것을 막으려면 주석을 해제하세요.
  public ApiResponse<String> getTestToken() {

    // 1. DB에 테스트용 유저가 없다면 강제로 하나 만듭니다. (ID: 1L)
    User testUser =
        userRepository
            .findById(1L)
            .orElseGet(
                () -> {
                  User newUser =
                      User.builder()
                          .socialId("TEST_KAKAO_12345")
                          .nickname("테스트유저")
                          .role(Role.USER) // 리뷰 작성을 위해 USER 권한 부여
                          .build();
                  return userRepository.save(newUser);
                });

    // 2. 해당 유저 객체를 그대로 넘겨 AccessToken 발급 (수정 완료!)
    String testAccessToken = jwtProvider.generateAccessToken(testUser);

    // 3. 발급된 토큰 반환 (스웨거 Authorize에 바로 복붙해서 사용)
    return ApiResponse.success(testAccessToken, "테스트용 토큰 발급 성공");
  }
}
