package com.kathena.backend.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kathena.backend.global.common.code.BaseErrorCode;
import com.kathena.backend.global.common.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.MDC;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final String traceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public ApiResponse(Boolean isSuccess, String code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
        this.traceId = MDC.get("traceId"); // 로그 트레이싱 ID 자동 매핑
    }

    // 성공
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, "COMMON200", "요청에 성공하였습니다.", result);
    }

    // 성공이지만 별도 메시지 필요
    public static <T> ApiResponse<T> onSuccess(String code, String message, T result) {
        return new ApiResponse<>(true, code, message, result);
    }

    //BaseErrorCode를 바로 받는 팩토리 메서드
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T data) {
        ErrorReasonDto reason = code.getReason();
        return new ApiResponse<>(false, reason.getCode(), reason.getMessage(), data);
    }

    // 데이터 없는 실패 응답
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code) {
        return onFailure(code, null);
    }

    // 실패
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}