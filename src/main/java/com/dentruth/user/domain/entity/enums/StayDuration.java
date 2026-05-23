package com.dentruth.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum StayDuration {

    UNDER_ONE_M("1개월 미만"),
    ONE_TO_THREE_M("1~3개월"),
    THREE_TO_SIX_M("3~6개월"),
    OVER_SIX_M("6개월 초과");

    private final String description;

    StayDuration(String description) {
        this.description = description;
    }

}
