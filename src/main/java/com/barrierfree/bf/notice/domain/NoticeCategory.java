package com.barrierfree.bf.notice.domain;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;

public enum NoticeCategory {
  ALL("전체", "전체"),
  NOTICE("공지", "공지"),
  UPDATE("업데이트", "업데이트"),
  MAINTENANCE("점검", "점검");

  private final String defaultText;
  private final String selectedText;

  NoticeCategory(String defaultText, String selectedText) {
    this.defaultText = defaultText;
    this.selectedText = selectedText;
  }

  public String getDefaultText() {
    return defaultText;
  }

  public String getSelectedText() {
    return selectedText;
  }

  public static NoticeCategory from(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String normalized = value.trim();
    return Arrays.stream(values())
        .filter(
            category ->
                category.name().equalsIgnoreCase(normalized)
                    || category.defaultText.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }

  public static NoticeCategory writableFrom(String value) {
    NoticeCategory category = from(value);
    if (category == ALL) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return category;
  }

  public static List<NoticeCategory> writableCategories() {
    return Arrays.stream(values()).filter(category -> category != ALL).toList();
  }
}
