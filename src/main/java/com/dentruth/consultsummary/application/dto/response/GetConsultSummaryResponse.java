package com.dentruth.consultsummary.application.dto.response;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Getter
@Slf4j
public class GetConsultSummaryResponse {

    private ConsultationInfo consultationInfo;
    private Diagnosis diagnosis;
    private List<TreatmentPlan> treatmentPlan;
    private List<TreatmentDelay> treatmentDelay;
    private List<TreatmentAfterCare> treatmentAfterCare;
    private String failReason;
    private String status;

    public static GetConsultSummaryResponse from(ConsultSummary consultSummary, JsonNode root) {
        ConsultationInfo info = ConsultationInfo.of(
                consultSummary.getClinicName(),
                consultSummary.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
        );

        if (consultSummary.getStatus() != SummaryStatus.COMPLETED) {
            return GetConsultSummaryResponse.builder()
                    .consultationInfo(info)
                    .status(consultSummary.getStatus().name())
                    .failReason(consultSummary.getFailReason())
                    .build();
        }

        return GetConsultSummaryResponse.builder()
                .consultationInfo(info)
                .diagnosis(Diagnosis.from(root))
                .treatmentPlan(TreatmentPlan.listFrom(root))
                .treatmentDelay(TreatmentDelay.listFrom(root))
                .treatmentAfterCare(TreatmentAfterCare.listFrom(root))
                .status(consultSummary.getStatus().name())
                .failReason(null)
                .build();
    }

    @Builder
    @Getter
    public static class ConsultationInfo {
        private String clinicName;
        private LocalDate date;

        public static ConsultationInfo of(String clinicName, LocalDate date) {
            return ConsultationInfo.builder()
                    .clinicName(clinicName)
                    .date(date)
                    .build();
        }
    }

    @Builder
    @Getter
    public static class Diagnosis {
        private String summary;
        private String summaryEng;
        private String description;
        private String descriptionEng;

        public static Diagnosis from(JsonNode root) {
            JsonNode node = root.path("diagnosis");
            if (node.isMissingNode() || node.isNull()) {
                return null;
            }
            return Diagnosis.builder()
                    .summary(node.path("summary").asText(""))
                    .summaryEng(node.path("summaryEng").asText(""))
                    .description(node.path("description").asText(""))
                    .descriptionEng(node.path("descriptionEng").asText(""))
                    .build();
        }
    }

    @Builder
    @Getter
    public static class TreatmentPlan {
        private int step;
        private String plan;
        private String planEng;

        public static List<TreatmentPlan> listFrom(JsonNode root) {
            JsonNode array = root.path("treatmentPlan");
            List<TreatmentPlan> result = new ArrayList<>();
            if (!array.isArray()) {
                return result;
            }
            for (JsonNode node : array) {
                result.add(TreatmentPlan.builder()
                        .step(node.path("step").asInt(0))
                        .plan(node.path("plan").asText(""))
                        .planEng(node.path("planEng").asText(""))
                        .build());
            }
            return result;
        }
    }

    @Builder
    @Getter
    public static class TreatmentDelay {
        private String title;
        private String titleEng;
        private String description;
        private String descriptionEng;

        public static List<TreatmentDelay> listFrom(JsonNode root) {
            JsonNode array = root.path("treatmentDelay");
            List<TreatmentDelay> result = new ArrayList<>();
            if (!array.isArray()) {
                return result;
            }
            for (JsonNode node : array) {
                result.add(TreatmentDelay.builder()
                        .title(node.path("title").asText(""))
                        .titleEng(node.path("titleEng").asText(""))
                        .description(node.path("description").asText(""))
                        .descriptionEng(node.path("descriptionEng").asText(""))
                        .build());
            }
            return result;
        }
    }

    @Builder
    @Getter
    public static class TreatmentAfterCare {
        private String title;
        private String titleEng;
        private String description;
        private String descriptionEng;

        public static List<TreatmentAfterCare> listFrom(JsonNode root) {
            JsonNode array = root.path("treatmentAfterCare");
            List<TreatmentAfterCare> result = new ArrayList<>();
            if (!array.isArray()) {
                return result;
            }
            for (JsonNode node : array) {
                result.add(TreatmentAfterCare.builder()
                        .title(node.path("title").asText(""))
                        .titleEng(node.path("titleEng").asText(""))
                        .description(node.path("description").asText(""))
                        .descriptionEng(node.path("descriptionEng").asText(""))
                        .build());
            }
            return result;
        }
    }

}
