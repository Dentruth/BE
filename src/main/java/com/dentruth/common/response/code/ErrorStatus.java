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

    // USER
    ALREADY_REGISTERED_EMAIL(HttpStatus.CONFLICT, "USER_002", "This email is already in use"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_003", "Email verification request failed. Please try again"),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "USER_004", "Passwords do not match"),
    SUSPENDED_USER(HttpStatus.FORBIDDEN, "USER_005", "일시 정지된 계정입니다."),
    BLOCKED_USER(HttpStatus.FORBIDDEN, "USER_006", "차단된 계정입니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "USER_007", "기존 비밀번호와 동일한 비밀번호입니다."),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "USER_008", "The verification code is incorrect"),
    EXPIRED_AUTH_CODE(HttpStatus.BAD_REQUEST, "USER_009", "The verification code has expired. Please request a new one"),
    UNAUTHORIZED_EMAIL_VERIFICATION(HttpStatus.FORBIDDEN, "USER_010", "Email verification has expired or access is invalid. Please verify again." ),

    //consult-summary,
    SUMMARY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "CON_001", "요약 기록 정보가 없습니다."),
    INVALID_ID_FORMAT(HttpStatus.BAD_REQUEST, "CON_002", "올바른 Id 형태가 아닙니다."),
    AUDIO_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "CON_003", "음성 파일 정보가 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "CON_004", "지원하지 않는 파일 형식입니다."),
    WHISPER_API_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CON_005", "Whisper STT 변환에 실패했습니다."),
    SUMMARIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CON_006", "AI 요약에 실패했습니다."),

    // schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_404", "일정을 찾을 수 없습니다."),
    INVALID_SCHEDULE_TIME(HttpStatus.BAD_REQUEST, "SCHEDULE_001", "시작 시간은 종료 시간보다 이전이어야 합니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
