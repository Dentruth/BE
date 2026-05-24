package com.dentruth.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum InsuranceStatus {

    INSURED("Insured", "가입"),
    NOT_INSURED("Uninsured", "미가입"),
    PLANNED("Planning to Enroll", "가입 예정");

    private final String eng;
    private final String ko;

    InsuranceStatus(String eng, String ko) {
        this.eng = eng;
        this.ko = ko;
    }

}
