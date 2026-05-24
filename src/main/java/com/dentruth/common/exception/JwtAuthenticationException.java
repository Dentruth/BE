package com.dentruth.common.exception;

import com.dentruth.common.response.code.ErrorStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtAuthenticationException extends RuntimeException {

    private final ErrorStatus errorCode;

    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }
}
