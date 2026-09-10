package com.barrierfree.bf.policy.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.policy.dto.PolicyCategoryResponse;
import com.barrierfree.bf.policy.dto.PolicyDocumentResponse;
import com.barrierfree.bf.policy.dto.PolicyDocumentUpdateRequest;
import com.barrierfree.bf.policy.service.PolicyDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Policy Document", description = "설정 화면 정책 문서 API")
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyDocumentController {

  private final PolicyDocumentService policyDocumentService;

  @GetMapping("/categories")
  @Operation(summary = "정책 문서 카테고리 조회", description = "설정 화면 탭에 노출할 정책 문서 카테고리 목록을 조회합니다.")
  public ApiResponse<List<PolicyCategoryResponse>> getCategories() {
    return ApiResponse.success(policyDocumentService.getCategories(), "정책 문서 카테고리 조회 성공");
  }

  @GetMapping
  @Operation(summary = "활성 정책 문서 전체 조회", description = "현재 활성화된 최신 정책 문서 목록을 조회합니다.")
  public ApiResponse<List<PolicyDocumentResponse>> getActivePolicies() {
    return ApiResponse.success(policyDocumentService.getActivePolicies(), "활성 정책 문서 목록 조회 성공");
  }

  @GetMapping("/{category}")
  @Operation(summary = "활성 정책 문서 상세 조회", description = "카테고리별 현재 활성화된 최신 정책 문서를 조회합니다.")
  public ApiResponse<PolicyDocumentResponse> getActivePolicy(
      @Parameter(description = "정책 문서 카테고리", example = "PRIVACY_POLICY") @PathVariable
          String category) {
    return ApiResponse.success(policyDocumentService.getActivePolicy(category), "활성 정책 문서 조회 성공");
  }

  @GetMapping("/{category}/versions")
  @Operation(summary = "정책 문서 버전 이력 조회", description = "카테고리별 정책 문서 버전 이력을 최신순으로 조회합니다.")
  public ApiResponse<List<PolicyDocumentResponse>> getPolicyVersions(
      @Parameter(description = "정책 문서 카테고리", example = "PRIVACY_POLICY") @PathVariable
          String category) {
    return ApiResponse.success(policyDocumentService.getPolicyVersions(category), "정책 문서 버전 이력 조회 성공");
  }

  @PatchMapping("/{category}")
  @Operation(summary = "[관리자] 정책 문서 수정", description = "새 버전을 발행하고 기존 활성 버전을 비활성화합니다.")
  public ApiResponse<PolicyDocumentResponse> publishPolicy(
      @Parameter(description = "정책 문서 카테고리", example = "PRIVACY_POLICY") @PathVariable
          String category,
      @Valid @RequestBody PolicyDocumentUpdateRequest request) {
    return ApiResponse.success(policyDocumentService.publishPolicy(category, request), "정책 문서 수정 성공");
  }
}
