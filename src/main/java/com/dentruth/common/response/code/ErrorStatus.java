package com.dentruth.common.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "회원 정보가 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근이 불가능합니다."),

    // AUTH
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "만료된 access token 입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 refresh token 입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 refresh token 입니다. 다시 로그인하세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 token 입니다."),

    // USER,
    ALREADY_REGISTERED_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 가입된 이메일입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_003", "이메일 인증 요청에 실패했습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "USER_004", "비밀번호가 일치하지 않습니다."),
    SUSPENDED_USER(HttpStatus.FORBIDDEN, "USER_005", "일시 정지된 계정입니다."),
    BLOCKED_USER(HttpStatus.FORBIDDEN, "USER_006", "차단된 계정입니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "USER_007", "기존 비밀번호와 동일한 비밀번호입니다."),


    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
  
}
