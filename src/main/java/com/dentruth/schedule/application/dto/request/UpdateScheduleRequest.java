package com.dentruth.schedule.application.dto.request;

import com.dentruth.schedule.domain.entity.enums.ClinicPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class UpdateScheduleRequest {

    @NotBlank
    private String clinicName;

    @NotNull
    private ClinicPurpose clinicPurpose;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private LocalTime endTime;

    private String memo;
}
