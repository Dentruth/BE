package com.dentruth.schedule.application.dto.response;

import com.dentruth.schedule.domain.entity.enums.ScheduleType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
public class MonthlyScheduleItemResponse {

    private Long id;

    private LocalDate date;

    private ScheduleType scheduleType;

    private String scheduleName;

    private Integer extraCount;
}