package com.barrierfree.bf.policy.domain;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Locale;

public enum PolicyCategory {
  TERMS_OF_SERVICE("이용약관", 1),
  PRIVACY_POLICY("개인정보처리방침", 2),
  LOCATION_BASED_SERVICE_TERMS("위치기반서비스 이용약관", 3),
  ACCESSIBILITY_INFO_DISCLAIMER("접근성 정보 안내 및 면책 고지", 4),
  COMMUNITY_POLICY("커뮤니티 운영 정책", 5);

  private final String label;
  private final int displayOrder;

  PolicyCategory(String label, int displayOrder) {
    this.label = label;
    this.displayOrder = displayOrder;
  }

  public String getLabel() {
    return label;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public static PolicyCategory from(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String normalized = normalize(value);
    return Arrays.stream(values())
        .filter(category -> normalize(category.name()).equals(normalized))
        .findFirst()
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-/]", "");
  }
}
