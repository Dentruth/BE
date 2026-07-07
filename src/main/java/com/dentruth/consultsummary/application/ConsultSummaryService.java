package com.dentruth.consultsummary.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import com.dentruth.consultsummary.application.dto.request.CreateConsultSummaryApplicationRequest;
import com.dentruth.consultsummary.application.dto.request.UpdateSummaryApplicationRequest;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultSummaryService {

    private final ConsultSummaryRepository consultSummaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ConsultSummary saveCreateConsultSummary(UUID userId, String audioLink, String clinicName) {
        ConsultSummary consultSummary = ConsultSummary.create(userId, audioLink, clinicName);
        return consultSummaryRepository.save(consultSummary);
    }

    @Transactional
    public ConsultSummary saveCreateConsultSummary(UUID userId, CreateConsultSummaryApplicationRequest request) {
        ConsultSummary consultSummary = ConsultSummary.create(userId, request.getAudioLink(), request.getClinicName(),
                request.getPractitionerName(), request.getLicenseType(), request.getLicenseNumber(),
                request.getInstitution(), request.getTreatmentPlan());
        return consultSummaryRepository.save(consultSummary);
    }

    @Transactional(readOnly = true)
    public ConsultSummary findById(UUID summaryId, UUID userId) {
        return consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> {
                    log.info("존재하지 않는 요약 정보 조회 요청. User Id : [{}]", userId);
                    return new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND);
                });
    }

    @Transactional
    public void markAsCompleted(UUID summaryId, SummarizedResult result) {
        ConsultSummary summary = consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND));

        summary.markAsCompleted(
                result.getRawJson(), result.getDiagnosis(), result.getTitle(), result.getTreatmentPlan());
    }

    @Transactional
    public void markAsFailed(UUID summaryId, String failReason) {
        ConsultSummary summary = consultSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND));
        summary.markAsFailed(failReason);
    }

    @Transactional(readOnly = true)
    public List<ConsultSummary> findAllSummaries(UUID userId, UUID cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        if (cursor == null) {
            return consultSummaryRepository.findFirstPage(userId, pageRequest);
        }

        ConsultSummary consultSummary = findById(cursor, userId);

        return consultSummaryRepository.findNextPage(userId, consultSummary.getCreatedAt(), cursor, pageRequest);
    }

    @Transactional
    public void deleteAll(List<UUID> summaryIds, UUID userId) {
        List<ConsultSummary> summaries = consultSummaryRepository.findAllById(summaryIds);

        if (summaries.size() != summaryIds.size()) {
            log.info("존재하지 않는 요약 내역을 포함한 삭제 요청. User Id : [{}]", userId);
            throw new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND);
        }

        summaries.forEach(s -> {
            if (!s.getUserId().equals(userId)) {
                log.info("본인 요약본이 아닌 요약본 삭제 요청. User Id : [{}], 요약본 id : [{}]", userId, s.getId());
                throw new DentruthException(ErrorStatus.FORBIDDEN);
            }
            s.delete();
        });
    }

    @Transactional
    public ConsultSummary update(UUID userId, UUID summaryId, UpdateSummaryApplicationRequest request) {
        ConsultSummary consultSummary = findById(summaryId, userId);

        if (consultSummary.getIsDeleted().equals(Boolean.TRUE)) {
            log.info("삭제된 요약 정보 수정 요청. User Id : [{}], summary Id : [{}]", userId, summaryId);
            throw new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND);
        }

        if (!consultSummary.getUserId().equals(userId)) {
            log.info("본인 요약본이 아닌 요약본 수정 요청. User Id : [{}], 요약본 id : [{}]", userId, summaryId);
            throw new DentruthException(ErrorStatus.FORBIDDEN);
        }

        String jsonResult = null;
        try {
            jsonResult = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("요약 수정본 데이터 JSON 직렬화 실패. Summary Id : [{}]", summaryId, e);
            throw new DentruthException(ErrorStatus.SUMMARIZATION_FAILED);
        }

        consultSummary.updateSummary(request.getClinicName(), request.getDiagnosis().getSummary(), jsonResult);
        return consultSummary;
    }

}
