package com.dentruth.consultsummary.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateConsultSummaryApplicationRequest {

    private String clinicName;
    private String audioLink;

}
