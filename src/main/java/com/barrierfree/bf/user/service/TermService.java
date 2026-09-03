package com.barrierfree.bf.user.service;

import com.barrierfree.bf.user.dto.TermCreateRequest;
import com.barrierfree.bf.user.dto.TermResponse;
import java.util.List;

public interface TermService {

    // [관리자] 새로운 약관 생성
    TermResponse createTerm(TermCreateRequest request);

    // [공통] 현재 활성화된(최신 버전의) 모든 약관 목록 조회
    List<TermResponse> getActiveTerms();

    // [공통] 특정 약관 상세 조회
    TermResponse getTerm(Long termId);
}
