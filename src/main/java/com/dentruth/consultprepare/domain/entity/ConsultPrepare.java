package com.dentruth.consultprepare.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.consultprepare.domain.entity.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultPrepare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private LocalDateTime appointmentDate;

    private String clinicName;

    @Enumerated(EnumType.STRING)
    private CurrentStatus currentStatus;

    private Boolean painExistence;

    private String painLocation;

    @Enumerated(EnumType.STRING)
    private PainLevel painLevel;

    @Enumerated(EnumType.STRING)
    private PainPersistence painPersistence;

    private String painDuration;

    private String worriedIssue;

    private String question;

    @Enumerated(EnumType.STRING)
    private SmokingLevel smoking;

    @Enumerated(EnumType.STRING)
    private DrinkingLevel drinking;

    @Enumerated(EnumType.STRING)
    private ExerciseLevel exercise;
}