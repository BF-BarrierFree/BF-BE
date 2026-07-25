package com.barrierfree.bf.global.enums;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityType {
  ELEVATOR("엘리베이터"),
  ELECTRIC_WHEELCHAIR_RENTAL("전동 휠체어 대여"),
  BRAILLE_BLOCK("점자블록"),
  SUBTITLE_SERVICE("자막 서비스"),
  ACCESSIBLE_RESTROOM("장애인 화장실"),
  WHEELCHAIR_SEAT("휠체어 좌석"),
  RAMP("경사로"),
  VOICE_GUIDANCE("음성안내"),
  ACCESSIBLE_PARKING("장애인 주차장"),
  REST_AREA("휴게공간"),
  SIGN_LANGUAGE_INTERPRETATION("수어통역"),
  NURSING_ROOM("수유실"),
  NONE("없음");

  private final String description;

  public static FacilityType from(String value) {
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
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }
}
