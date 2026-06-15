package com.dentruth.schedule.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DetailScheduleResponse {

    private List<CustomScheduleItemResponse> custom;

    private List<ConsultationScheduleItemResponse> consultation;
}
