package com.barrierfree.bf.auth.service;

import com.barrierfree.bf.auth.client.KakaoOAuthClient;
import com.barrierfree.bf.auth.dto.AuthResponse;
import com.barrierfree.bf.auth.dto.KakaoUserInfoResponse;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 프론트엔드로부터 인가 코드를 받아 카카오 토큰 발급 -> 회원 조회/가입 -> 자체 JWT 발급을 수행합니다.
     */
    @Transactional
    public AuthResponse kakaoLogin(String code) {
        // 1. 카카오 서버로부터 Access Token 발급
        String kakaoAccessToken = kakaoOAuthClient.getAccessToken(code);

        // 2. 발급받은 Token으로 유저 프로필 조회
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);
        String socialId = String.valueOf(kakaoUserInfo.id());

        // 3. DB에서 유저 조회 및 가입 처리
        Optional<User> optionalUser = userRepository.findBySocialIdAndIsDeletedFalse(socialId);

        User user;
        boolean isNewUser = false;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            log.info("기존 유저 로그인 성공: {}", user.getNickname());
        } else {
            // 신규 유저인 경우 GUEST 권한으로 가입 (온보딩 필요)
            user = registerNewUser(socialId); // kakaoUserInfo 파라미터 제거
            isNewUser = true;
            log.info("신규 유저 가입 완료: {}", user.getNickname());
        }

        // 4. 서비스 자체 JWT 토큰 생성 (실제 JwtProvider 연동)
        String appAccessToken = jwtProvider.generateAccessToken(user);
        String appRefreshToken = jwtProvider.generateRefreshToken(user);

        // 유저 DB에 Refresh Token 업데이트
        user.updateRefreshToken(appRefreshToken);

        // 5. 프론트엔드 응답 반환 (엔티티가 아닌 DTO로 변환하여 반환)
        return AuthResponse.builder()
            .accessToken(appAccessToken)
            .refreshToken(appRefreshToken)
            .role(user.getRole())
            .isNewUser(isNewUser)
            .build();
    }

    /**
     * 신규 유저 생성 헬퍼 메서드
     * 카카오 프로필 정보를 수집하지 않는 기획에 따라, 소셜 고유 ID만으로 유저를 생성합니다.
     */
    private User registerNewUser(String socialId) {
        // 기획에 맞게 닉네임과 프로필 이미지는 온보딩 또는 자체 로직으로 처리
        // DB 테이블 제약조건에 따라 초기 닉네임은 랜덤 문자열(UUID 등)이나 "신규유저" 등으로 임시 할당할 수 있습니다.
        String temporaryNickname = "무장애여행자_" + socialId.substring(Math.max(0, socialId.length() - 4));

        User newUser = User.builder()
            .socialId(socialId)
            .nickname(temporaryNickname) // 임시 닉네임 부여 (온보딩 시 수정)
            .profileImageUrl(null)       // 기본 프로필 이미지는 프론트에서 처리하거나 NULL
            .role(Role.GUEST)            // 온보딩 전이므로 GUEST 권한 부여
            .build();

        return userRepository.save(newUser);
    }

    /**
     * 로그아웃 처리: DB에 저장된 Refresh Token을 제거합니다.
     * (클라이언트 측에서는 로컬 스토리지 등에 있는 토큰을 비워야 합니다.)
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.clearRefreshToken();
    }
}
