package com.barrierfree.bf.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 결과에 포함하지 않음
public class ApiResponse<T> {

  // HTTP 상태 코드와 별개로, 우리 서비스만의 고유 비즈니스 코드 (예: "SUCCESS", "ERR_MAP_001")
  private final String code;
  private final String message;
  private final T data;

  /** 성공 응답 (데이터 반환 시) */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("SUCCESS", "요청에 성공하였습니다.", data);
  }

  /** 성공 응답 (커스텀 메시지 포함) */
  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>("SUCCESS", message, data);
  }

  /** 성공 응답 (데이터가 필요 없는 POST, PUT, DELETE 요청 시) */
  public static ApiResponse<?> successWithNoContent() {
    return new ApiResponse<>("SUCCESS", "요청에 성공하였습니다.", null);
  }

  /** 에러 응답 (GlobalExceptionHandler 등에서 에러 코드와 함께 반환 시) */
  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
