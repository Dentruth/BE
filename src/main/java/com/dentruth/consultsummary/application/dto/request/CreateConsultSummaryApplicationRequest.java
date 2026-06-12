package com.dentruth.consultsummary.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateConsultSummaryApplicationRequest {

    private String clinicName;
    private String audioLink;
    private String practitionerName;
    private String licenseType;
    private String licenseNumber;
    private String institution;

}
