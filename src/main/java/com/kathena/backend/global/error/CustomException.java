package com.kathena.backend.global.error;

import com.kathena.backend.global.common.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode code;

    public CustomException(BaseErrorCode code) {
        super(code.getReason().getMessage());
        this.code = code;
    }
}