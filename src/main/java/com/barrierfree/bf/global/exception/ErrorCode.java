package com.barrierfree.bf.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // --- 공통 에러 (Global) ---
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "잘못된 입력값입니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G002", "서버 내부 오류가 발생했습니다."),

  // --- 무장애 시설 관련 에러 (Facility) ---
  FACILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "해당 시설을 찾을 수 없습니다."),

  // --- 외부 API 관련 에러 (Map) ---
  GOOGLE_MAP_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "M001", "구글맵 API 호출에 실패했습니다."),
  GOOGLE_MAP_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "M002", "구글맵 API 응답이 지연되고 있습니다.");

  private final HttpStatus status; // HTTP 상태 코드 (ex: 400, 404, 500)
  private final String code; // 우리 서비스만의 고유 코드
  private final String message; // 사용자에게 보여줄 메시지
}
