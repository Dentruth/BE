package com.dentruth.schedule.application.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
public class HomeScheduleResponse {
    private Long id;
    private LocalDate date;
    private String scheduleName;
    private Integer extraCount;
}
