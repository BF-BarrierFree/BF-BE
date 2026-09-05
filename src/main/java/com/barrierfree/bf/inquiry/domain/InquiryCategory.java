package com.barrierfree.bf.inquiry.domain;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;

public enum InquiryCategory {
  SERVICE_USAGE("서비스 이용", "서비스이용"),
  COURSE_ERROR("코스 오류", "코스오류"),
  ACCESSIBILITY_INFO_ERROR("접근성 정보 오류", "접근성정보오류"),
  CONTENT("콘텐츠 문의", "콘텐츠문의"),
  ETC("기타");

  private final String text;
  private final String[] aliases;

  InquiryCategory(String text, String... aliases) {
    this.text = text;
    this.aliases = aliases;
  }

  public String getText() {
    return text;
  }

  public static InquiryCategory from(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String normalized = normalize(value);
    return Arrays.stream(values())
        .filter(
            category ->
                normalize(category.name()).equals(normalized)
                    || normalize(category.text).equals(normalized)
                    || Arrays.stream(category.aliases)
                        .anyMatch(alias -> normalize(alias).equals(normalized)))
        .findFirst()
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }

  private static String normalize(String value) {
    return value.trim().replaceAll("[\\s_-]", "").toUpperCase();
  }
}
