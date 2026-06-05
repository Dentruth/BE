package com.dentruth.schedule.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.schedule.domain.entity.enums.ClinicPurpose;
import com.dentruth.schedule.domain.entity.enums.ScheduleType;
import com.dentruth.schedule.presentation.dto.request.CreateScheduleRequest;
import com.dentruth.user.domain.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import lombok.*;

@Entity
@Getter
@Builder
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    private String scheduleName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime endTime;

    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    @JoinColumn(name = "user_id", nullable = false)
    private UUID userId;

    public static Schedule createCustomSchedule(
            UUID userId,
            CreateScheduleRequest request
    ) {

        validateScheduleTime(
                request.getStartDate(),
                request.getStartTime(),
                request.getEndDate(),
                request.getEndTime()
        );

        return Schedule.builder()
                .clinicName(request.getClinicName())
                .clinicPurpose(request.getClinicPurpose())
                .scheduleName(request.getScheduleName())
                .startDate(request.getStartDate())
                .startTime(request.getStartTime())
                .endDate(request.getEndDate())
                .endTime(request.getEndTime())
                .memo(request.getMemo())
                .scheduleType(ScheduleType.CUSTOM)
                .userId(userId)
                .build();
    }

    public void updateSchedule(
            String clinicName,
            ClinicPurpose clinicPurpose,
            String scheduleName,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime,
            String memo,
            ScheduleType scheduleType
    ) {

        validateScheduleTime(
                startDate,
                startTime,
                endDate,
                endTime
        );

        this.clinicName = clinicName;
        this.clinicPurpose = clinicPurpose;
        this.scheduleName = scheduleName;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.memo = memo;
        this.scheduleType = scheduleType;
    }

    private static void validateScheduleTime(
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime
    ) {

        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);

        if (start.isAfter(end)) {
            throw new DentruthException(ErrorStatus.INVALID_SCHEDULE_TIME);
        }
    }
}
