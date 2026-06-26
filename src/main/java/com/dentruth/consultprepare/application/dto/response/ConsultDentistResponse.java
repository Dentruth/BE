package com.dentruth.consultprepare.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConsultDentistResponse {

    private String insuranceStatus;
    private PainSummary painSummary;
    private SummaryInfo summaryInfo;
    private String visitPurpose;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PainSummary {

        private String painKo;

        private String painOrigin;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SummaryInfo {

        private String stayStatus;

        private String painLocation;

        private String painInfo;

        private String dentalHistory;

        private String medicalHistory;

        private String socialHistory;
    }

}
