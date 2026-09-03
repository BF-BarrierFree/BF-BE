package com.barrierfree.bf.user.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.user.dto.*;
import com.barrierfree.bf.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 API", description = "온보딩 및 사용자 정보 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "사용자 온보딩",
      description = "GUEST 권한의 사용자가 추가 정보(닉네임, 이동 유형, 필요시설, 약관 동의)를 입력하고 USER 권한으로 전환합니다.")
  @PostMapping("/onboarding")
  public ApiResponse<OnboardingResponse> completeOnboarding(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Valid @RequestBody OnboardingRequest request) {

    OnboardingResponse response = userService.onboarding(userId, request);
    return ApiResponse.success(response, "온보딩이 성공적으로 완료되었습니다.");
  }

  @Operation(summary = "내 프로필 조회", description = "현재 로그인한 유저의 프로필 정보를 조회합니다.")
  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> getMyProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
    UserProfileResponse response = userService.getMyProfile(userId);
    return ApiResponse.success(response);
  }

  @Operation(summary = "내 프로필 수정", description = "닉네임, 이동 수단, 필요 시설 정보를 수정합니다.")
  @PatchMapping("/me")
  public ApiResponse<?> updateMyProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserUpdateRequest request) {
    userService.updateMyProfile(userId, request);
    return ApiResponse.successWithNoContent();
  }

  @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 진행합니다. (개인정보는 마스킹 처리됩니다.)")
  @DeleteMapping("/me")
  public ApiResponse<?> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
    userService.withdraw(userId);
    return ApiResponse.success(null, "회원 탈퇴가 완료되었습니다.");
  }

  @Operation(summary = "내 선호 필터 조회", description = "로그인 사용자의 온보딩 결과를 조회해 필터 초기값으로 사용할 수 있게 합니다.")
  @GetMapping("/me/preferences")
  public ApiResponse<UserPreferenceResponse> getMyPreferences(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

    UserPreferenceResponse response = userService.getPreferences(userId);
    return ApiResponse.success(response, "사용자 선호값 조회에 성공했습니다.");
  }

    @Operation(summary = "내 선호 필터 수정", description = "장애유형 및 필요 시설 필터 정보만 단독으로 가볍게 부분 수정(Patch)합니다.")
    @PatchMapping("/me/preferences")
    public ApiResponse<?> updateMyPreferences(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @RequestBody UserPreferenceUpdateRequest request) {

        userService.updateMyPreferences(userId, request);
        return ApiResponse.successWithNoContent();
    }

}
