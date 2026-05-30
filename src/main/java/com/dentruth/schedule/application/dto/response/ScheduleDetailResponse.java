package com.dentruth.schedule.application.dto.response;

import com.dentruth.schedule.domain.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleDetailResponse {

    private Long scheduleId;
    private String clinicName;
    private String clinicPurpose;
    private String scheduleName;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String memo;

    public static ScheduleDetailResponse from(Schedule schedule) {

        return ScheduleDetailResponse.builder()
                .scheduleId(schedule.getId())
                .clinicName(schedule.getClinicName())
                .clinicPurpose(schedule.getClinicPurpose().name())
                .scheduleName(schedule.getScheduleName())
                .startDate(schedule.getStartDate())
                .startTime(schedule.getStartTime())
                .endDate(schedule.getEndDate())
                .endTime(schedule.getEndTime())
                .memo(schedule.getMemo())
                .build();
    }
}
