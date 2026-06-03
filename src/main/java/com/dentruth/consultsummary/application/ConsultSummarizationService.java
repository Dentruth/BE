package com.dentruth.consultsummary.application;

import com.dentruth.consultsummary.application.dto.SummarizedResult;

public interface ConsultSummarizationService {
    SummarizedResult summarize(String transcribedText, String clinicName, String date);
}
