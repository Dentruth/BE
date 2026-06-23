package com.dentruth.consultsummary.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.CursorResponse;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.request.CreateConsultSummaryApplicationRequest;
import com.dentruth.consultsummary.application.dto.request.UpdateSummaryApplicationRequest;
import com.dentruth.consultsummary.application.dto.response.ConsultSummariesResponse;
import com.dentruth.consultsummary.application.dto.response.CreateConsultSummaryResponse;
import com.dentruth.consultsummary.application.dto.response.GetConsultSummaryResponse;
import com.dentruth.consultsummary.application.dto.response.PresignedUrlResponse;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultSummaryFacade {

    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final ConsultSummaryService consultSummaryService;
    private final TranscriptionEventPublisher transcriptionEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PresignedUrlResponse getUploadUrl(String filename, String contentType, UUID userId) {
        log.info("Presigned URL 발급 요청. filename: [{}], contentType: [{}], User Id : [{}]",
                filename, contentType, userId);

        findUser(userId, "Presigned URL 발급 요청");

        return fileStorageService.generateUploadUrl(filename, contentType, userId);
    }

    public CreateConsultSummaryResponse createConsultSummary(UUID userId,
                                                             CreateConsultSummaryApplicationRequest request) {
        log.info("상담 요약 기록 생성 요청. userId : [{}]", userId);
        findUser(userId, "상담 요약 기록 생성");

        ConsultSummary savedSummary =
                consultSummaryService.saveCreateConsultSummary(userId, request.getAudioLink(), request.getClinicName());

        transcriptionEventPublisher.publish(savedSummary.getId(), request.getAudioLink(), request.getClinicName(),
                convertToLocalDateTime(savedSummary.getCreatedAt()).toString());

        return CreateConsultSummaryResponse.builder()
                .id(savedSummary.getId())
                .clinicName(savedSummary.getClinicName())
                .status(savedSummary.getStatus())
                .createdAt(convertToLocalDateTime(savedSummary.getCreatedAt()))
                .build();
    }

    public CreateConsultSummaryResponse createConsultSummaryV2(UUID userId,
                                                               CreateConsultSummaryApplicationRequest request) {
        log.info("상담 요약 기록 생성 요청. userId : [{}]", userId);
        findUser(userId, "상담 요약 기록 생성");

        ConsultSummary savedSummary =
                consultSummaryService.saveCreateConsultSummary(userId, request);

        transcriptionEventPublisher.publish(savedSummary.getId(), request.getAudioLink(), request.getClinicName(),
                convertToLocalDateTime(savedSummary.getCreatedAt()).toString());

        return CreateConsultSummaryResponse.builder()
                .id(savedSummary.getId())
                .clinicName(savedSummary.getClinicName())
                .status(savedSummary.getStatus())
                .createdAt(convertToLocalDateTime(savedSummary.getCreatedAt()))
                .build();
    }

    public GetConsultSummaryResponse getDetail(UUID userId, UUID consultSummaryId) {
        findUser(userId, "ai 요약 내용 조회");
        ConsultSummary consultSummary = consultSummaryService.findById(consultSummaryId, userId);

        if (!consultSummary.getUserId().equals(userId)) {
            log.info("본인 소유가 아닌 요약 내용 조회 요청. User Id : [{}]. summary Id : [{}]", userId, consultSummaryId);
            throw new DentruthException(ErrorStatus.FORBIDDEN);
        }

        if (consultSummary.getIsDeleted().equals(Boolean.TRUE)) {
            throw new DentruthException(ErrorStatus.SUMMARY_RECORD_NOT_FOUND);
        }

        JsonNode root = consultSummary.getStatus() == SummaryStatus.COMPLETED
                ? parseJson(consultSummary.getDiagnosticResult(), consultSummaryId)
                : null;

        return GetConsultSummaryResponse.from(consultSummary, root);
    }

    public CursorResponse<ConsultSummariesResponse> getConsultSummaries(UUID userId, UUID cursor, int size) {
        findUser(userId, "ai 요약 기록 전체 조회");
        List<ConsultSummary> allSummaries = consultSummaryService.findAllSummaries(userId, cursor, size);

        return getConsultSummariesResponseCursorResponse(size, allSummaries);
    }

    private CursorResponse<ConsultSummariesResponse> getConsultSummariesResponseCursorResponse(int size,
                                                                                               List<ConsultSummary> allSummaries) {
        boolean hasNext = allSummaries.size() > size;

        List<ConsultSummary> page = hasNext ? allSummaries.subList(0, size) : allSummaries;

        List<ConsultSummariesResponse> items = page.stream()
                .map(ConsultSummariesResponse::from)
                .toList();

        String nextCursor = (hasNext && !page.isEmpty()) ?
                page.get(page.size() - 1).getId().toString() : null;

        return CursorResponse.of(items, nextCursor, hasNext);
    }

    public void deleteSummaries(UUID userId, List<UUID> summaryIds) {
        log.info("ai 녹음 요약 삭제 요청. User Id : [{}]", userId);
        findUser(userId, "ai 녹음 요약 삭제 요청");
        consultSummaryService.deleteAll(summaryIds, userId);
    }

    public GetConsultSummaryResponse updateSummary(UUID userId, UUID id, UpdateSummaryApplicationRequest request) {
        log.info("ai 녹음 요약 수정 요청. User Id : [{}]", userId);
        findUser(userId, "ai 녹음 요약 수정 요청");
        ConsultSummary updated = consultSummaryService.update(userId, id, request);

        JsonNode root = updated.getStatus() == SummaryStatus.COMPLETED
                ? parseJson(updated.getDiagnosticResult(), id)
                : null;

        return GetConsultSummaryResponse.from(updated, root);
    }

    public void findUser(UUID userId, String method) {
        User user = userService.findById(userId, method);
        user.validateStatus();
    }

    private LocalDateTime convertToLocalDateTime(Instant time) {
        return time.atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    private JsonNode parseJson(String json, UUID summaryId) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Diagnostic Result Json 파싱 실패. Summary Id : [{}]", summaryId, e);
            throw new DentruthException(ErrorStatus.SUMMARIZATION_FAILED);
        }
    }

}
