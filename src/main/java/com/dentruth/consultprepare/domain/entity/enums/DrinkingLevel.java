package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkingLevel {

    NON_DRINK("Non-drinker", "비음주", "Non Drinker"),
    OCCASIONAL("Occasional Drinker", "가끔 음주", "Alcohol Once a Week"),
    REGULAR("Regular Drinker", "음주", "Alcohol Several Times a Week"),
    HEAVY("Heavy Drinker", "잦은 음주", "Alcohol Daily");

    private final String eng;
    private final String ko;
    private final String description;
}
