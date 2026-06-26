package com.dentruth.consultprepare.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PainPersistence {

    ONGOING("지속됨", "Persistent pain"),
    OCCASIONAL("간헐적", "Occasional pain"),
    RESOLVED("사라짐", "Pain has resolved");

    private final String ko;
    private final String eng;
}
