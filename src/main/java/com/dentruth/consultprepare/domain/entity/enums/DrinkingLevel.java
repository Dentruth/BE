package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkingLevel {

    NON_SMOKER("Non-drinker", "비음주"),
    OCCASIONAL("Occasional Drinker", "가끔 음주"),
    REGULAR("Regular Drinker", "음주"),
    HEAVY("Heavy Drinker", "잦은 음주");

    private final String eng;
    private final String ko;
}
