package com.dentruth.common.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_SERVER_ERROR("COMMON_500", "서버 에러입니다."),
    BAD_REQUEST("COMMON_400", "잘못된 요청입니다."),

    EXPIRED_ACCESS_TOKEN("AUTH_001", "만료된 access token 입니다."),
    EXPIRED_REFRESH_TOKEN("AUTH_002", "만료된 refresh token 입니다."),
    INVALID_REFRESH_TOKEN("AUTH_003", "유효하지 않은 refresh token 입니다. 다시 로그인하세요."),
    INVALID_TOKEN("AUTH_004", "유효하지 않은 token 입니다.");


    private final String code;
    private final String message;
}
