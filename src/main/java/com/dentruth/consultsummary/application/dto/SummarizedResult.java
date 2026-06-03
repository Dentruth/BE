package com.dentruth.consultsummary.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
public class SummarizedResult {

    private final String rawJson;
    private final String diagnosis;
    private final String treatmentPlan;
    private final String title;

}
