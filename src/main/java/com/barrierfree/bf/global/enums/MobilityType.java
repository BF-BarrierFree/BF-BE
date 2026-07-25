package com.barrierfree.bf.global.enums;

import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MobilityType {
  WHEELCHAIR("휠체어 이용자"),
  STROLLER("유아차 동반"),
  COGNITIVE_DEVELOPMENTAL("인지/발달 장애"),
  VISUAL_IMPAIRMENT("시각 장애"),
  HEARING_IMPAIRMENT("청각 장애"),
  OTHER("기타");

  private final String description;

  public static MobilityType from(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(
            type ->
                type.name().equalsIgnoreCase(normalized)
                    || type.description.equals(value.trim())
                    || type.description.equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow();
  }
}
