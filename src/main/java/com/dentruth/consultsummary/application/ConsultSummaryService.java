package com.dentruth.consultsummary.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultSummaryService {

    private final ConsultSummaryRepository consultSummaryRepository;

    @Transactional
    public ConsultSummary saveCreateConsultSummary(UUID userId, String audioLink, String clinicName) {
        ConsultSummary consultSummary = ConsultSummary.create(userId, audioLink, clinicName);
        return consultSummaryRepository.save(consultSummary);
    }

    @Transactional(readOnly = true)
    public ConsultSummary findById(UUID summaryId){
        return consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND));
    }

    @Transactional
    public void markAsCompleted(UUID summaryId, SummarizedResult result) {
        ConsultSummary summary = consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND));

        summary.markAsCompleted(
                result.getRawJson(), result.getDiagnosis(), result.getTitle());
    }

    @Transactional
    public void markAsFailed(UUID summaryId, String failReason) {
        ConsultSummary summary = consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND));
        summary.markAsFailed(failReason);
    }
}
