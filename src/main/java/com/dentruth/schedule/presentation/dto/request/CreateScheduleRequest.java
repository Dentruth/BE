package com.dentruth.schedule.presentation.dto.request;

import com.dentruth.schedule.domain.entity.enums.ClinicPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class CreateScheduleRequest {

    @NotBlank(message = "병원명은 필수입니다.")
    private String clinicName;

    @NotNull(message = "진료 목적은 필수입니다.")
    private ClinicPurpose clinicPurpose;

    @NotBlank(message = "일정명은 필수입니다.")
    private String scheduleName;

    @NotNull(message = "시작 날짜는 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "시작 시간은 필수입니다.")
    private LocalTime startTime;

    @NotNull(message = "종료 날짜는 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "종료 시간은 필수입니다.")
    private LocalTime endTime;

    private String memo;
}
