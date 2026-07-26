package com.barrierfree.bf.user.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.user.dto.OnboardingRequest;
import com.barrierfree.bf.user.dto.OnboardingResponse;
import com.barrierfree.bf.user.dto.UserProfileResponse;
import com.barrierfree.bf.user.dto.UserUpdateRequest;
import com.barrierfree.bf.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API 엔드포인트를 처리하는 컨트롤러
 */
@Tag(name = "사용자 API", description = "온보딩 및 사용자 정보 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GUEST 유저의 온보딩(추가 정보 입력 및 약관 동의)을 처리합니다.
     *
     * @param userId  인증 필터를 통과한 현재 유저의 PK (SecurityContext에서 주입)
     * @param request 닉네임, 다중 선택 정보(이동 유형, 필요 시설), 약관 동의 내역
     * @return 권한이 USER로 승격된 새로운 토큰 정보
     */
    @Operation(summary = "신규 유저 온보딩", description = "GUEST 권한 유저의 추가 정보(닉네임, 다중 선택, 약관 동의)를 입력받고 USER 권한으로 승격합니다.")
    @PostMapping("/onboarding")
    public ApiResponse<OnboardingResponse> completeOnboarding(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId, // Swagger 입력창에서 숨김 처리
        @Valid @RequestBody OnboardingRequest request
    ) {
        // 비즈니스 로직 처리 (온보딩 및 권한 승격)
        OnboardingResponse response = userService.onboarding(userId, request);

        // 프론트엔드에서 띄워줄 알림창을 고려하여 친절한 메시지와 함께 반환
        return ApiResponse.success(response, "온보딩이 성공적으로 완료되었습니다.");
    }

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 유저의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponse response = userService.getMyProfile(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임, 이동 수단, 필요 시설 정보를 수정합니다.")
    @PatchMapping("/me")
    public ApiResponse<?> updateMyProfile(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        userService.updateMyProfile(userId, request);
        return ApiResponse.successWithNoContent();
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 진행합니다. (개인정보는 마스킹 처리됩니다.)")
    @DeleteMapping("/me")
    public ApiResponse<?> withdraw(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        userService.withdraw(userId);
        return ApiResponse.success(null, "회원 탈퇴가 완료되었습니다.");
    }
}
