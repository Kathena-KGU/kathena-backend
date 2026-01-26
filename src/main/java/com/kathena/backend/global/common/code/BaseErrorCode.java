package com.kathena.backend.global.common.code;

public interface BaseErrorCode {
    // 에러 정보 반환
    ErrorReasonDto getReason();

    // HTTP 상태 코드까지 포함하여 반환
    ErrorReasonDto getReasonHttpStatus();
}