package com.barrierfree.bf.global.enums;

/**
 * 사용자의 시스템 권한을 정의하는 Enum
 * 카카오 로그인 직후 온보딩을 거치지 않은 유저와 완료한 유저를 구분합니다.
 */
public enum Role {
    
    // Spring Security 권한 규칙에 따라 'ROLE_' 접두사를 포함하는 것을 권장합니다.
    GUEST("ROLE_GUEST", "온보딩 미완료 사용자"),
    USER("ROLE_USER", "온보딩 완료 정식 사용자"),
    ADMIN("ROLE_ADMIN", "시스템 관리자");

    private final String key;
    private final String title;

    Role(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }
}