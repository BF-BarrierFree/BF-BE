package com.barrierfree.bf.user.service;

import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.OnboardingRequest;
import com.barrierfree.bf.user.dto.OnboardingResponse;
import com.barrierfree.bf.user.entity.Term;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.entity.UserTermAgreement;
import com.barrierfree.bf.user.repository.TermRepository;
import com.barrierfree.bf.user.repository.UserRepository;
import com.barrierfree.bf.user.repository.UserTermAgreementRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;
    private final JwtProvider jwtProvider;

    /**
     * GUEST 유저의 온보딩(추가 정보 입력 및 약관 동의)을 처리하고 USER 권한으로 승격합니다.
     *
     * @param userId  인증된 현재 유저의 PK
     * @param request 프론트엔드에서 전달한 온보딩 입력 폼 데이터
     * @return 새로운 토큰과 승격된 권한 정보가 담긴 응답 DTO
     */
    @Transactional
    public OnboardingResponse onboarding(Long userId, OnboardingRequest request) {
        // 1. 유저 조회
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 닉네임 중복 검증
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 3. 약관 동의 처리 (필수 약관 누락 검증 및 매핑 테이블 저장)
        processTermAgreements(user, request.getAgreedTermIds());

        // 4. 온보딩 정보 업데이트 (닉네임, 다중 선택 항목) 및 권한 승격(GUEST -> USER)
        user.completeOnboarding(request.getNickname(), request.getMobilities(), request.getFacilities());

        // 5. 권한이 USER로 승격되었으므로 새로운 Access/Refresh JWT 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // 새 Refresh Token DB 반영
        user.updateRefreshToken(newRefreshToken);

        // 6. 응답 DTO 반환
        return OnboardingResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .nickname(user.getNickname())
            .role(user.getRole())
            .build();
    }

    /**
     * 프론트엔드에서 넘어온 동의한 약관 ID 목록을 바탕으로 필수 약관 검증 및 DB 저장을 수행합니다.
     */
    private void processTermAgreements(User user, List<Long> agreedTermIds) {
        // DB에 등록된 활성화된 전체 약관 조회
        List<Term> activeTerms = termRepository.findByIsActiveTrue();

        // 등록된 필수 약관 목록이 유저가 넘긴 agreedTermIds에 모두 포함되어 있는지 확인
        boolean hasAllRequiredTerms = activeTerms.stream()
            .filter(Term::isRequired)
            .allMatch(term -> agreedTermIds.contains(term.getId()));

        if (!hasAllRequiredTerms) {
            throw new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }

        // 유저가 체크(동의)한 약관만 필터링하여 매핑 엔티티 생성 후 Bulk Save
        List<UserTermAgreement> agreements = activeTerms.stream()
            .filter(term -> agreedTermIds.contains(term.getId()))
            .map(term -> UserTermAgreement.builder()
                .user(user)
                .term(term)
                .isAgreed(true)
                .build())
            .collect(Collectors.toList());

        userTermAgreementRepository.saveAll(agreements);
    }
}
