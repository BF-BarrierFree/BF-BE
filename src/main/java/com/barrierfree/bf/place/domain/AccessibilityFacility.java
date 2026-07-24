package com.barrierfree.bf.place.domain;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum AccessibilityFacility {
  ELEVATOR("엘리베이터"),
  RAMP("경사로"),
  ACCESSIBLE_RESTROOM("장애인 화장실"),
  SIGN_LANGUAGE("수어통역"),
  ACCESSIBLE_PARKING("장애인 주차장"),
  SUBTITLE_SERVICE("자막 서비스"),
  WHEELCHAIR_RENTAL("전동 휠체어 대여"),
  NURSING_ROOM("수유실"),
  WHEELCHAIR_SEAT("휠체어 좌석"),
  REST_AREA("휴게공간"),
  VOICE_GUIDANCE("음성안내"),
  BRAILLE_BLOCK("점자블록");

  private final String label;

  AccessibilityFacility(String label) {
    this.label = label;
  }

  public static AccessibilityFacility from(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String normalized = value.trim();
    return Arrays.stream(values())
        .filter(facility -> facility.name().equalsIgnoreCase(normalized) || facility.label.equals(normalized))
        .findFirst()
        .orElseThrow();
  }
}
