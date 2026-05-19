package com.dentruth.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum InsuranceStatus {

    INSURED("가입"),
    NOT_INSURED("미가입"),
    PLANNED("가입 예정");

    private final String description;

    InsuranceStatus(String description) {
        this.description = description;
    }

}
