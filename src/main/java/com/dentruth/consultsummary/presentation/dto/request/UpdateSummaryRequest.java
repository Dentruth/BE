package com.dentruth.consultsummary.presentation.dto.request;

import com.dentruth.consultsummary.application.dto.request.UpdateSummaryApplicationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UpdateSummaryRequest {

    @Size.List({
            @Size(min = 2, message = "Must be at least 2 characters"),
            @Size(max = 100, message = "Cannot exceed 100 characters")
    })
    private String clinicName;

    @Valid
    private Diagnosis diagnosis;

    @Valid
    private List<TreatmentPlan> treatmentPlan;

    @Valid
    private List<TreatmentDelay> treatmentDelay;

    @Valid
    private List<TreatmentAfterCare> treatmentAfterCare;

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Diagnosis {

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 60, message = "Cannot exceed 60 characters")
        })
        private String summary;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 60, message = "Cannot exceed 60 characters")
        })
        private String summaryEng;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String description;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String descriptionEng;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TreatmentPlan {

        private Integer step;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String plan;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String planEng;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TreatmentDelay {

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 100, message = "Cannot exceed 100 characters")
        })
        private String title;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 100, message = "Cannot exceed 100 characters")
        })
        private String titleEng;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String description;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String descriptionEng;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TreatmentAfterCare {

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 100, message = "Cannot exceed 100 characters")
        })
        private String title;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 100, message = "Cannot exceed 100 characters")
        })
        private String titleEng;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String description;

        @Size.List({
                @Size(min = 2, message = "Must be at least 2 characters"),
                @Size(max = 300, message = "Cannot exceed 300 characters")
        })
        private String descriptionEng;
    }

    public UpdateSummaryApplicationRequest toApplicationRequest() {
        return UpdateSummaryApplicationRequest.builder()
                .clinicName(this.clinicName)
                .diagnosis(this.diagnosis != null ? UpdateSummaryApplicationRequest.Diagnosis.builder()
                        .summary(this.diagnosis.getSummary())
                        .summaryEng(this.diagnosis.getSummaryEng())
                        .description(this.diagnosis.getDescription())
                        .descriptionEng(this.diagnosis.getDescriptionEng())
                        .build() : null)
                .treatmentPlan(this.treatmentPlan != null ? this.treatmentPlan.stream()
                        .map(plan -> UpdateSummaryApplicationRequest.TreatmentPlan.builder()
                                .step(plan.getStep())
                                .plan(plan.getPlan())
                                .planEng(plan.getPlanEng())
                                .build())
                        .toList() : List.of())
                .treatmentDelay(this.treatmentDelay != null ? this.treatmentDelay.stream()
                        .map(delay -> UpdateSummaryApplicationRequest.TreatmentDelay.builder()
                                .title(delay.getTitle())
                                .titleEng(delay.getTitleEng())
                                .description(delay.getDescription())
                                .descriptionEng(delay.getDescriptionEng())
                                .build())
                        .toList() : List.of())
                .treatmentAfterCare(this.treatmentAfterCare != null ? this.treatmentAfterCare.stream()
                        .map(afterCare -> UpdateSummaryApplicationRequest.TreatmentAfterCare.builder()
                                .title(afterCare.getTitle())
                                .titleEng(afterCare.getTitleEng())
                                .description(afterCare.getDescription())
                                .descriptionEng(afterCare.getDescriptionEng())
                                .build())
                        .toList() : List.of())
                .build();
    }

}
