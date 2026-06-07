package com.dentruth.consultprepare.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.consultprepare.domain.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultPrepare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime appointmentDate;

    private String clinicName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrentStatus currentStatus;

    @Column(nullable = false)
    private Boolean painExistence;

    private String painLocation;

    @Enumerated(EnumType.STRING)
    private PainLevel painLevel;

    @Enumerated(EnumType.STRING)
    private PainPersistence painPersistence;

    private String painDuration;

    private String worriedIssue;

    private String question;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SmokingLevel smoking;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DrinkingLevel drinking;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExerciseLevel exercise;

}
