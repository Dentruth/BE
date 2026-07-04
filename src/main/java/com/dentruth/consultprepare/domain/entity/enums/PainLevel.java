package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PainLevel {

    NONE("통증 없음", "No pain", "None"),
    MILD("약함", "Mild", "Mild"),
    MODERATE("보통", "Moderate", "Moderate"),
    SEVERE("심함", "Severe", "Severe");

    private final String ko;
    private final String eng;
    private final String description;

}
