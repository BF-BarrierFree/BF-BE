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
  GOOGLE_MAP_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "M002", "구글맵 API 응답이 지연되고 있습니다."),

  // --- 길찾기 및 경로 탐색 관련 에러 (Route) ---
  ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "요청하신 조건으로 이동 가능한 휠체어 경로를 찾을 수 없습니다."),
  INVALID_ROUTE_COORDINATES(
      HttpStatus.BAD_REQUEST, "R002", "출발지 또는 도착지의 좌표가 올바르지 않습니다. (예: 바다 한가운데)"),
  EXTERNAL_ROUTING_FAILED(
      HttpStatus.SERVICE_UNAVAILABLE, "R003", "길찾기 서버와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.");

  private final HttpStatus status; // HTTP 상태 코드
  private final String code; // 서비스 고유 에러 코드
  private final String message; // 사용자(프론트엔드)에게 보여줄 친절한 알림 메시지
}
