package com.dentruth.schedule.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MonthlyScheduleResponse {

    private List<LocalDate> scheduleDates;

    private List<MonthlyScheduleItemResponse> schedules;
}
