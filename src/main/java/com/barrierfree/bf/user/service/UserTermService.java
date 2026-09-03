package com.barrierfree.bf.user.service;

import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.dto.UserTermAgreementResponse;
import java.util.List;

public interface UserTermService {

    // 1. 특정 사용자의 전체 약관 동의 내역 조회
    List<UserTermAgreementResponse> getUserAgreements(Long userId);

    // 2. [온보딩/마이페이지] 사용자의 약관 동의 상태 업데이트 (단건 혹은 다건)
    void updateAgreements(Long userId, TermAgreementUpdateRequest request);

    // 3. 필수 약관에 모두 동의했는지 검증 (온보딩 완료 전 체크용)
    boolean checkRequiredTermsAgreed(Long userId);

    // 4. [온보딩] 유저의 최초 약관 동의 내역 저장 및 필수 약관 검증
    void saveOnboardingAgreements(Long userId, java.util.List<Long> agreedTermIds);
}
