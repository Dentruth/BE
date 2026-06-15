package com.dentruth.schedule.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CustomScheduleItemResponse {

    private Long id;

    private LocalDate date;

    private LocalTime time;

    private String clinicName;

    private String scheduleName;

    private String memo;
}
