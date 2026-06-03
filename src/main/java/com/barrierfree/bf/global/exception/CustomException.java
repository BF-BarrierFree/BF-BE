package com.barrierfree.bf.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    // 만약 기본 메시지 대신 다른 메시지를 던지고 싶을 때 사용
    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }
}