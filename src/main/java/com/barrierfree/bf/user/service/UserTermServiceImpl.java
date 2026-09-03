package com.barrierfree.bf.user.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.dto.UserTermAgreementResponse;
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
@Transactional(readOnly = true)
public class UserTermServiceImpl implements UserTermService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;

    @Override
    public List<UserTermAgreementResponse> getUserAgreements(Long userId) {
        // 사용자 검증 로직 추가 가능

        List<UserTermAgreement> agreements = userTermAgreementRepository.findByUserId(userId);
        return agreements.stream()
            .map(UserTermAgreementResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateAgreements(Long userId, TermAgreementUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        for (TermAgreementUpdateRequest.TermAgreementDto dto : request.getAgreements()) {
            Term term = termRepository.findById(dto.getTermId())
                .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND));

            // 필수 약관인데 동의를 취소(false)하려는 경우 에러 처리
            if (term.isRequired() && !dto.getIsAgreed()) {
                throw new CustomException(ErrorCode.REQUIRED_TERM_CANCELLATION_NOT_ALLOWED);
            }

            // 기존 동의 내역 조회
            UserTermAgreement agreement = userTermAgreementRepository.findByUserIdAndTermId(userId, term.getId())
                .orElseGet(() -> UserTermAgreement.builder()
                    .user(user)
                    .term(term)
                    .isAgreed(dto.getIsAgreed())
                    .build());

            // 상태 업데이트 (기존 내역이 있으면 값 변경, 없으면 새로 생성된 객체의 값 설정)
            agreement.updateAgreement(dto.getIsAgreed());

            userTermAgreementRepository.save(agreement); // JPA Save (or Update)
        }
    }

    @Override
    public boolean checkRequiredTermsAgreed(Long userId) {
        // 활성화된 필수 약관 목록 조회
        List<Term> requiredTerms = termRepository.findAllByIsRequiredTrueAndIsActiveTrue();

        // 사용자가 동의한 약관 내역 조회
        List<UserTermAgreement> userAgreements = userTermAgreementRepository.findByUserId(userId);

        // 동의한 필수 약관 ID 목록 추출 (isAgreed가 true인 것만)
        List<Long> agreedRequiredTermIds = userAgreements.stream()
            .filter(UserTermAgreement::isAgreed)
            .filter(agreement -> agreement.getTerm().isRequired())
            .map(agreement -> agreement.getTerm().getId())
            .toList();

        // 전체 필수 약관 ID가 사용자가 동의한 필수 약관 ID 목록에 모두 포함되는지 확인
        return requiredTerms.stream()
            .allMatch(term -> agreedRequiredTermIds.contains(term.getId()));
    }

    @Override
    @Transactional
    public void saveOnboardingAgreements(Long userId, List<Long> agreedTermIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // DB에 등록된 활성화된 전체 약관 조회
        List<Term> activeTerms = termRepository.findAllByIsActiveTrue();

        // 활성화된 약관 ID 목록 추출
        List<Long> activeTermIds = activeTerms.stream().map(Term::getId).toList();

        // 유저가 동의한 모든 약관 ID가 활성화된 약관에 존재하는지 검증
        boolean allAgreedTermsAreValid = agreedTermIds.stream().allMatch(activeTermIds::contains);

        if (!allAgreedTermsAreValid) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

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
            .map(term -> UserTermAgreement.builder().user(user).term(term).isAgreed(true).build())
            .collect(Collectors.toList());

        userTermAgreementRepository.saveAll(agreements);
    }
}
