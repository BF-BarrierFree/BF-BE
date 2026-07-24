package com.barrierfree.bf.user.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 무장애 지도 서비스의 사용자 정보를 관리하는 Entity
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (무분별한 객체 생성 방지)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카카오에서 제공하는 고유 식별자 (중복 가입 방지용)
    @Column(nullable = false, unique = true)
    private String socialId;

    @Column(nullable = false, length = 50)
    private String nickname;

    // 카카오 프로필 이미지 URL (기본 제공 이미지가 있을 수 있음)
    @Column(length = 500)
    private String profileImageUrl;

    // 온보딩 완료 여부에 따라 GUEST / USER 권한 분리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 사용자의 이동 유형 (다중 선택 가능, Nullable)
    @ElementCollection
    @CollectionTable(name = "user_mobilities", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_type")
    private List<MobilityType> mobilities = new ArrayList<>();

    // 사용자의 선호 접근성 시설 (다중 선택 가능, Nullable)
    @ElementCollection
    @CollectionTable(name = "user_facilities", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type")
    private List<FacilityType> facilities = new ArrayList<>();

    // Refresh Token 저장 (Redis 대체, MVP 용도)
    @Column(length = 500)
    private String refreshToken;

    // --- 약관 동의 (PM 기획 반영 전 실무 표준 구조) ---
    // 필수 약관 동의 여부 (서비스 이용약관, 개인정보 수집 등)
    @Column(nullable = false)
    private boolean isRequiredTermsAgreed = false;
    
    // 위치정보 수집 동의 (선택이지만 지도 서비스라 중요함)
    @Column(nullable = false)
    private boolean isLocationTermsAgreed = false;
    
    // 선택 마케팅 수신 동의 여부
    @Column(nullable = false)
    private boolean isMarketingAgreed = false;

    // 약관 동의 일시
    private LocalDateTime termsAgreedAt;
    // ----------------------------------------------------

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @Builder
    public User(String socialId, String nickname, String profileImageUrl, Role role) {
        this.socialId = socialId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    /**
     * 회원가입(최초 로그인) 후 온보딩 시 약관 동의 내역을 업데이트합니다.
     */
    public void agreeTerms(boolean isRequiredTermsAgreed, boolean isLocationTermsAgreed, boolean isMarketingAgreed) {
        this.isRequiredTermsAgreed = isRequiredTermsAgreed;
        this.isLocationTermsAgreed = isLocationTermsAgreed;
        this.isMarketingAgreed = isMarketingAgreed;
        this.termsAgreedAt = LocalDateTime.now();
    }

    /**
     * 로그인 시 Refresh Token을 업데이트합니다.
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 회원이 온보딩을 완료하고 추가 정보를 기입했을 때 호출됩니다.
     * 권한이 GUEST에서 USER로 승급됩니다.
     */
    public void completeOnboarding(List<MobilityType> mobilities, List<FacilityType> facilities) {
        this.mobilities = mobilities != null ? mobilities : new ArrayList<>();
        this.facilities = facilities != null ? facilities : new ArrayList<>();
        this.role = Role.USER;
    }

    /**
     * 회원 탈퇴 시 호출되는 Soft Delete 메서드입니다.
     * 개인정보 보호를 위해 닉네임과 프로필 사진을 마스킹/초기화합니다.
     *
     * @param maskedNickname "삭제된사용자_UUID" 형태의 마스킹된 닉네임
     * @param defaultImageUrl 서비스의 기본 프로필 이미지 URL
     */
    public void softDelete(String maskedNickname, String defaultImageUrl) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.nickname = maskedNickname;
        this.profileImageUrl = defaultImageUrl;
        this.socialId = "DELETED_" + this.id; // 동일 소셜 계정 재가입 허용을 위한 조치
        this.refreshToken = null;
        this.mobilities.clear();
        this.facilities.clear();
    }
}