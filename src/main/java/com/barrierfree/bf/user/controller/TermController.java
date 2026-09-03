package com.barrierfree.bf.user.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.user.dto.TermCreateRequest;
import com.barrierfree.bf.user.dto.TermResponse;
import com.barrierfree.bf.user.service.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "약관 API", description = "서비스 약관 조회 및 관리(Admin) API")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @Operation(summary = "[관리자] 새 약관 생성", description = "새로운 약관 개정판을 생성하여 등록합니다.")
    @PostMapping
    public ApiResponse<TermResponse> createTerm(@Valid @RequestBody TermCreateRequest request) {
        TermResponse response = termService.createTerm(request);
        return ApiResponse.success(response, "새로운 약관이 성공적으로 등록되었습니다.");
    }

    @Operation(summary = "활성 약관 전체 조회", description = "현재 유효한 최신 버전의 모든 약관 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<TermResponse>> getActiveTerms() {
        List<TermResponse> response = termService.getActiveTerms();
        return ApiResponse.success(response, "활성화된 약관 목록 조회에 성공했습니다.");
    }

    @Operation(summary = "특정 약관 상세 조회", description = "약관 ID를 기반으로 특정 약관의 상세 내용을 조회합니다.")
    @GetMapping("/{termId}")
    public ApiResponse<TermResponse> getTerm(@PathVariable Long termId) {
        TermResponse response = termService.getTerm(termId);
        return ApiResponse.success(response, "약관 상세 조회에 성공했습니다.");
    }
}
