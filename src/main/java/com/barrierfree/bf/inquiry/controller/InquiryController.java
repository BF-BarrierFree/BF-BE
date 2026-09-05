package com.barrierfree.bf.inquiry.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.inquiry.dto.InquiryAnswerRequest;
import com.barrierfree.bf.inquiry.dto.InquiryCreateRequest;
import com.barrierfree.bf.inquiry.dto.InquiryResponse;
import com.barrierfree.bf.inquiry.dto.InquirySummaryResponse;
import com.barrierfree.bf.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiries")
@Tag(name = "Inquiry", description = "1:1 문의 API")
public class InquiryController {

  private final InquiryService inquiryService;

  @Operation(summary = "1:1 문의 접수")
  @PostMapping
  public ApiResponse<InquiryResponse> createInquiry(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Valid @RequestBody InquiryCreateRequest request) {
    return ApiResponse.success(inquiryService.createInquiry(userId, request), "문의가 접수되었습니다.");
  }

  @Operation(summary = "내 문의 목록 조회")
  @GetMapping("/my")
  public ApiResponse<Page<InquirySummaryResponse>> getMyInquiries(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ApiResponse.success(inquiryService.getMyInquiries(userId, pageable));
  }

  @Operation(summary = "내 문의 상세 조회")
  @GetMapping("/{inquiryId}")
  public ApiResponse<InquiryResponse> getMyInquiry(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @PathVariable Long inquiryId) {
    return ApiResponse.success(inquiryService.getMyInquiry(userId, inquiryId));
  }

  @Operation(summary = "문의 답변 등록")
  @PatchMapping("/{inquiryId}/answer")
  public ApiResponse<InquiryResponse> answerInquiry(
      @Parameter(hidden = true) @AuthenticationPrincipal Long adminId,
      @PathVariable Long inquiryId,
      @Valid @RequestBody InquiryAnswerRequest request) {
    return ApiResponse.success(
        inquiryService.answerInquiry(adminId, inquiryId, request), "문의 답변이 등록되었습니다.");
  }
}
