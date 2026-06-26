package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PainLevel {

    NONE("통증 없음", "No pain"),
    MILD("약함", "Mild"),
    MODERATE("보통", "Moderate"),
    SEVERE("심함", "Severe");

    private final String ko;
    private final String eng;

}
