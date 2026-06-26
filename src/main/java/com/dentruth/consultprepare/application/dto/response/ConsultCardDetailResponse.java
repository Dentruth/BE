package com.dentruth.consultprepare.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class ConsultCardDetailResponse {

    private LocalDate consultDate;

    private String stayStatus;

    private String painArea;

    private String painLevelDuration;

    private String socialHistory;

    private List<String> dentalHistories;

    private List<String> medicalHistories;

    private String concerns;

    private List<String> recommendedQuestions;

}
