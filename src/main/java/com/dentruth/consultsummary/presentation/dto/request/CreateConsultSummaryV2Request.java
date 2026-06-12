package com.dentruth.consultsummary.presentation.dto.request;

import com.dentruth.consultsummary.application.dto.request.CreateConsultSummaryApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class CreateConsultSummaryV2Request {

    @NotBlank(message = "병원 이름은 필수입니다.")
    @Size(min = 2, max = 100, message = "병원 이름은 2~100자 사이여야 합니다.")
    private String clinicName;

    @NotBlank(message = "음성 파일은 필수입니다.")
    private String audioLink;

    @NotBlank(message = "의료인 성명은 필수입니다.")
    private String practitionerName;

    @NotBlank(message = "면허 종류는 필수입니다.")
    private String licenseType;

    private String licenseNumber;
    private String institution;

    public CreateConsultSummaryApplicationRequest toApplicationRequest(){
        return CreateConsultSummaryApplicationRequest.builder()
                .clinicName(this.clinicName)
                .audioLink(this.audioLink)
                .practitionerName(this.practitionerName)
                .licenseType(this.licenseType)
                .licenseNumber(this.licenseNumber)
                .institution(this.institution)
                .build();
    }

}
