package com.barrierfree.bf.user.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.dto.UserTermAgreementResponse;
import com.barrierfree.bf.user.service.UserTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 약관 API", description = "내 약관 동의 조회 및 변경")
@RestController
@RequestMapping("/api/v1/users/me/terms")
@RequiredArgsConstructor
public class UserTermController {
  private final UserTermService userTermService;

  @Operation(
      summary = "내 약관 동의 조회",
      description = "활성 약관 전체의 동의 상태를 조회합니다. 동의 이력이 없는 약관은 미동의로 반환합니다.")
  @GetMapping
  public ApiResponse<List<UserTermAgreementResponse>> getMyAgreements(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
    return ApiResponse.success(userTermService.getUserAgreements(userId));
  }

  @Operation(
      summary = "내 약관 동의 변경",
      description =
          "요청한 활성 약관의 동의 상태만 변경합니다. 선택 약관은 동의하거나 철회할 수 있으며 필수 약관은 철회할 수 없습니다. 여러 항목은 함께 성공하거나 함께 취소됩니다.")
  @PatchMapping
  public ApiResponse<?> updateMyAgreements(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Valid @RequestBody TermAgreementUpdateRequest request) {
    userTermService.updateAgreements(userId, request);
    return ApiResponse.successWithNoContent();
  }
}
