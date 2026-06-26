package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SmokingLevel {

    NON_SMOKER("Non-smoker", "비흡연"),
    OCCASIONAL("Occasional Smoker", "가끔 흡연"),
    REGULAR("Regular Smoker", "흡연"),
    HEAVY("Heavy Smoker", "많이 흡연");

    private final String eng;
    private final String ko;
}
