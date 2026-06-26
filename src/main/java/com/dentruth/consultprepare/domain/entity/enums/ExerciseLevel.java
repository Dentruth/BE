package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseLevel {
    NON_SMOKER("No Exercise", "운동 안 함"),
    OCCASIONAL("Occasional Exercise", "가끔 운동"),
    REGULAR("Regular Exercise", "규칙적으로 운동"),
    HEAVY("Heavy Exercise", "강도 높은 운동");

    private final String eng;
    private final String ko;
}
