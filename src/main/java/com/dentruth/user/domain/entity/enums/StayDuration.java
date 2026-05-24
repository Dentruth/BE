package com.dentruth.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum StayDuration {

    UNDER_ONE_M("Under 1 Month", "1개월 미만"),
    ONE_TO_THREE_M("1–3 Months","1~3개월"),
    THREE_TO_SIX_M("3–6 Months", "3~6개월"),
    OVER_SIX_M("6 Months+", "6개월 초과");

    private final String eng;
    private final String ko;

    StayDuration(String eng, String ko) {
        this.eng = eng;
        this.ko = ko;
    }

}
