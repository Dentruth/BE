package com.dentruth.consultsummary.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummarizedResult {

    private final String rawJson;
    private final String diagnosis;
    private final String title;
    private final String treatmentPlan;

}
