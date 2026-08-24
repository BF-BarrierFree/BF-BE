package com.barrierfree.bf.notice.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.notice.dto.NoticeCategoryResponse;
import com.barrierfree.bf.notice.dto.NoticeCreateRequest;
import com.barrierfree.bf.notice.dto.NoticeResponse;
import com.barrierfree.bf.notice.dto.NoticeUpdateRequest;
import com.barrierfree.bf.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
@Tag(name = "Notice", description = "공지사항 API")
public class NoticeController {

  private final NoticeService noticeService;

  @Operation(summary = "공지사항 카테고리 조회")
  @GetMapping("/categories")
  public ApiResponse<List<NoticeCategoryResponse>> getCategories() {
    return ApiResponse.success(noticeService.getCategories());
  }

  @Operation(summary = "공지사항 목록 조회")
  @GetMapping
  public ApiResponse<Page<NoticeResponse>> getNotices(
      @Parameter(description = "카테고리: ALL, NOTICE, UPDATE, MAINTENANCE 또는 한글명")
          @RequestParam(required = false)
          String category,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ApiResponse.success(noticeService.getNotices(category, pageable));
  }

  @Operation(summary = "공지사항 상세 조회")
  @GetMapping("/{noticeId}")
  public ApiResponse<NoticeResponse> getNotice(@PathVariable Long noticeId) {
    return ApiResponse.success(noticeService.getNotice(noticeId));
  }

  @Operation(summary = "공지사항 생성")
  @PostMapping
  public ApiResponse<NoticeResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request) {
    return ApiResponse.success(noticeService.createNotice(request));
  }

  @Operation(summary = "공지사항 수정")
  @PutMapping("/{noticeId}")
  public ApiResponse<NoticeResponse> updateNotice(
      @PathVariable Long noticeId, @Valid @RequestBody NoticeUpdateRequest request) {
    return ApiResponse.success(noticeService.updateNotice(noticeId, request));
  }

  @Operation(summary = "공지사항 삭제")
  @DeleteMapping("/{noticeId}")
  public ApiResponse<?> deleteNotice(@PathVariable Long noticeId) {
    noticeService.deleteNotice(noticeId);
    return ApiResponse.successWithNoContent();
  }
}
