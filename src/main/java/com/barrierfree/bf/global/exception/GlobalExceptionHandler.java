package com.barrierfree.bf.global.exception;

import com.barrierfree.bf.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 프로젝트 전역에서 발생하는 예외를 잡는 어노테이션
public class GlobalExceptionHandler {

  /**
   * 우리가 만든 CustomException이 발생했을 때 처리 (Service 계층에서 throw new
   * CustomException(ErrorCode.FACILITY_NOT_FOUND); 할 때 잡힘)
   */
  @ExceptionHandler(CustomException.class)
  protected ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
    log.error("CustomException: {}", e.getErrorCode().getMessage());
    ErrorCode errorCode = e.getErrorCode();

    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }

  /** Validation (@Valid) 검사 실패 시 처리 (Controller에서 DTO 입력값 형식이 틀렸을 때 잡힘) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    log.error("MethodArgumentNotValidException: {}", e.getMessage());

    // 첫 번째 에러 메시지만 가져옴
    String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(errorCode.getCode(), errorMessage));
  }

  /** Validation for constrained controller method parameters, such as request parameters. */
  @ExceptionHandler(HandlerMethodValidationException.class)
  protected ResponseEntity<ApiResponse<?>> handleHandlerMethodValidationException(
      HandlerMethodValidationException e) {
    log.error("HandlerMethodValidationException: {}", e.getMessage());

    String errorMessage = e.getAllErrors().get(0).getDefaultMessage();
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(errorCode.getCode(), errorMessage));
  }

  /** 데이터베이스 제약 조건 위반 시 처리 (unique constraint 등) 닉네임 중복 등 트랜잭션 커밋 시점에 발생하는 제약 조건 위반을 처리합니다. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    log.error("DataIntegrityViolationException: {}", e.getMessage());

    // 닉네임 unique constraint 위반인 경우 NICKNAME_DUPLICATED 반환
    if (e.getMessage() != null && e.getMessage().contains("nickname")) {
      ErrorCode errorCode = ErrorCode.NICKNAME_DUPLICATED;
      return ResponseEntity.status(errorCode.getStatus())
          .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // 그 외 데이터 무결성 위반은 일반 에러로 처리
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }

  /** 그 외 예상치 못한 모든 에러 (NullPointerException 등) */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    log.error("Unhandled Exception: ", e);
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }
}
