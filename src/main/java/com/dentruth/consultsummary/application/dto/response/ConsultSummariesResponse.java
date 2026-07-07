package com.dentruth.consultsummary.application.dto.response;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultSummariesResponse {

    private final UUID id;
    private final String title;
    private final String clinicName;
    private final LocalDate date;
    private final String diagnosis;
    private final String status;
    private final String treatmentPlan;

    public static ConsultSummariesResponse from(ConsultSummary consultSummary) {
        return ConsultSummariesResponse.builder()
                .id(consultSummary.getId())
                .title(consultSummary.getTitle())
                .clinicName(consultSummary.getClinicName())
                .date(LocalDate.ofInstant(consultSummary.getCreatedAt(), ZoneId.of("Asia/Seoul")))
                .diagnosis(consultSummary.getDiagnosis())
                .status(consultSummary.getStatus().name())
                .treatmentPlan(consultSummary.getTreatmentPlan())
                .build();
    }

}
