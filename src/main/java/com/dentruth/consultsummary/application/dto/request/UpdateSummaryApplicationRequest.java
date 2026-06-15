package com.dentruth.consultsummary.application.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSummaryApplicationRequest {

    private final String clinicName;
    private final Diagnosis diagnosis;
    private final List<TreatmentPlan> treatmentPlan;
    private final List<TreatmentDelay> treatmentDelay;
    private final List<TreatmentAfterCare> treatmentAfterCare;

    @Getter
    @Builder
    public static class Diagnosis {
        private final String summary;
        private final String summaryEng;
        private final String description;
        private final String descriptionEng;
    }

    @Getter
    @Builder
    public static class TreatmentPlan {

        private final Integer step;
        private final String plan;
        private final String planEng;
    }

    @Getter
    @Builder
    public static class TreatmentDelay {
        private final String title;
        private final String titleEng;
        private final String description;
        private final String descriptionEng;
    }

    @Getter
    @Builder
    public static class TreatmentAfterCare {
        private final String title;
        private final String titleEng;
        private final String description;
        private final String descriptionEng;
    }

}
