package com.dentruth.common.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseCode {

    OK(HttpStatus.OK, "COMMON_200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON_201", "생성 성공."),
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON_202", "요청 성공."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
  
}
