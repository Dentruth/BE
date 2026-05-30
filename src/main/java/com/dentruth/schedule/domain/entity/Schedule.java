package com.dentruth.schedule.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.schedule.domain.entity.enums.ClinicPurpose;
import com.dentruth.user.domain.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clinicName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClinicPurpose clinicPurpose;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime endTime;

    private String memo;

    @JoinColumn(name = "user_id", nullable = false)
    private UUID userId;

    @Builder
    public Schedule(
            String clinicName,
            ClinicPurpose clinicPurpose,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime,
            String memo,
            UUID userId
    ) {
        this.clinicName = clinicName;
        this.clinicPurpose = clinicPurpose;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.memo = memo;
        this.userId = userId;
    }

    public void updateSchedule(
            String clinicName,
            ClinicPurpose clinicPurpose,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime,
            String memo
    ) {
        this.clinicName = clinicName;
        this.clinicPurpose = clinicPurpose;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.memo = memo;
    }
}
