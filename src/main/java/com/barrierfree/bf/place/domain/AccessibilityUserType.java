package com.barrierfree.bf.place.domain;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum AccessibilityUserType {
  WHEELCHAIR_USER("휠체어 이용자"),
  STROLLER("유아차 동반"),
  COGNITIVE_DEVELOPMENTAL_DISABILITY("인지/발달 장애"),
  VISUAL_IMPAIRED("시각 장애"),
  HEARING_IMPAIRED("청각 장애");

  private final String label;

  AccessibilityUserType(String label) {
    this.label = label;
  }

  public static AccessibilityUserType from(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalized = value.trim();
    return Arrays.stream(values())
        .filter(type -> type.name().equalsIgnoreCase(normalized) || type.label.equals(normalized))
        .findFirst()
        .orElseThrow();
  }
}
