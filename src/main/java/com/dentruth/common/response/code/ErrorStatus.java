package com.dentruth.common.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_SERVER_ERROR("COMMON_500", "서버 에러입니다."),
    BAD_REQUEST("COMMON_400", "잘못된 요청입니다.");

    private final String code;
    private final String message;
}
