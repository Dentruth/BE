package com.dentruth.consultprepare.application.dto.request;

import com.dentruth.consultprepare.domain.entity.enums.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConsultCardRequest {

    private String title;

    private VisitInfo visitInfo;
    private SymptomInfo symptomInfo;
    private MedicalHistories medicalHistories;
    private MemoInfo memoInfo;
    private List<VisitPurpose> purposes;

    @Getter @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisitInfo {

        private LocalDate visitDate;
        private CurrentStatus currentStatus;
    }

    @Getter @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymptomInfo {

        private Boolean painExistence;

        private PainPersistence painPersistence;

        private String painArea;

        private PainLevel painLevel;

        private String painDuration;
    }

    @Getter @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalHistories {

        private List<String> dentalHistories;

        private List<String> medicalHistories;

        private SocialHistory socialHistory;
    }

    @Getter @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialHistory {

        private SmokingLevel smoking;

        private DrinkingLevel drinking;

        private ExerciseLevel exercise;
    }

    @Getter @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoInfo {

        private String concerns;

        private String question;
    }
}
