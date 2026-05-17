package com.dentruth.common.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseCode {

    OK("COMMON_200", "성공입니다.");

    private final String code;
    private final String message;
}