package com.barrierfree.bf.auth.service;

import com.barrierfree.bf.auth.client.KakaoOAuthClient;
import com.barrierfree.bf.auth.dto.AuthResponse;
import com.barrierfree.bf.auth.dto.KakaoUserInfoResponse;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
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
            user = registerNewUser(socialId, kakaoUserInfo);
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
     */
    private User registerNewUser(String socialId, KakaoUserInfoResponse kakaoUserInfo) {
        String nickname = kakaoUserInfo.kakaoAccount().profile().nickname();
        String profileImageUrl = kakaoUserInfo.kakaoAccount().profile().profileImageUrl();

        User newUser = User.builder()
            .socialId(socialId)
            .nickname(nickname)
            .profileImageUrl(profileImageUrl)
            .role(Role.GUEST) // 온보딩 전이므로 GUEST 권한 부여
            .build();

        return userRepository.save(newUser);
    }
}