package com.barrierfree.bf.user.service;

import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.OnboardingRequest;
import com.barrierfree.bf.user.dto.OnboardingResponse;
import com.barrierfree.bf.user.dto.UserPreferenceResponse;
import com.barrierfree.bf.user.dto.UserProfileResponse;
import com.barrierfree.bf.user.dto.UserUpdateRequest;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserTermService userTermService;
    private final JwtProvider jwtProvider;

    /** GUEST 유저의 온보딩(추가 정보 입력 및 약관 동의)을 처리하고 USER 권한으로 승격합니다. */
    @Transactional
    public OnboardingResponse onboarding(Long userId, OnboardingRequest request) {
        // 1. 유저 조회
        User user =
            userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 온보딩 자격 검증 (GUEST 유저만 온보딩 가능)
        if (user.getRole() != Role.GUEST) {
            throw new CustomException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        // 3. 닉네임 중복 검증
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 4. 약관 동의 처리 (UserTermService로 역할 위임)
        userTermService.saveOnboardingAgreements(userId, request.getAgreedTermIds());

        // 5. 온보딩 정보 업데이트 (닉네임, 다중 선택 항목) 및 권한 승격(GUEST -> USER)
        user.completeOnboarding(
            request.getNickname(), request.getMobilities(), request.getFacilities());

        // 6. 권한이 USER로 승격되었으므로 새로운 Access/Refresh JWT 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // 새 Refresh Token DB 반영
        user.updateRefreshToken(newRefreshToken);

        // 7. 응답 DTO 반환
        return OnboardingResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .nickname(user.getNickname())
            .role(user.getRole())
            .build();
    }

    /** 내 프로필 정보(마이페이지)를 조회합니다. */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user =
            userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.builder()
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .role(user.getRole())
            .mobilities(user.getMobilities())
            .facilities(user.getFacilities())
            .build();
    }

    /** 내 프로필 정보(닉네임, 다중 선택 항목)를 수정합니다. */
    @Transactional
    public void updateMyProfile(Long userId, UserUpdateRequest request) {
        User user =
            userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 닉네임과 다를 경우에만 중복 검사 수행
        if (!user.getNickname().equals(request.getNickname())
            && userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        user.updateProfile(request.getNickname(), request.getMobilities(), request.getFacilities());
    }

    /** 내 선호 필터 정보(온보딩 결과)를 조회합니다. */
    @Transactional(readOnly = true)
    public UserPreferenceResponse getPreferences(Long userId) {
        User user =
            userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserPreferenceResponse(
            user.getId(),
            user.getNickname(),
            user.getRole(),
            user.getMobilities(),
            user.getFacilities());
    }

    /** 회원 탈퇴 처리를 수행합니다. (Soft Delete 및 개인정보 마스킹) */
    @Transactional
    public void withdraw(Long userId) {
        User user =
            userRepository
                .findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴한 유저의 닉네임을 "삭제된사용자_UUID" 형태로 마스킹하여 다른 유저가 해당 닉네임을 사용할 수 있도록 함
        String maskedNickname = "탈퇴유저_" + java.util.UUID.randomUUID().toString();

        user.softDelete(maskedNickname, null);
    }
}
