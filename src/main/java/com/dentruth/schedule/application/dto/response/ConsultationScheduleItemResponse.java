package com.dentruth.schedule.application.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
public class ConsultationScheduleItemResponse {

    private Long id;

    private LocalDate date;

    private String clinicName;

    private String scheduleName;
}
