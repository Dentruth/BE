package com.dentruth.consultsummary.presentation.dto.request;

import com.dentruth.consultsummary.application.dto.request.CreateConsultSummaryApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class CreateConsultSummaryRequest {

    @NotBlank(message = "병원 이름은 필수입니다.")
    @Size(min = 2, max = 100, message = "병원 이름은 2~100자 사이여야 합니다. ")
    private String clinicName;

    @NotBlank(message = "음성 파일은 필수입니다.")
    private String audioLink;

    public CreateConsultSummaryApplicationRequest toApplicationRequest(){
        return CreateConsultSummaryApplicationRequest.builder()
                .clinicName(this.clinicName)
                .audioLink(this.audioLink)
                .build();
    }

}
