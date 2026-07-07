package com.dentruth.consultsummary.infra.scheduler;

import com.dentruth.consultsummary.application.ConsultSummaryService;
import com.dentruth.consultsummary.application.TranscriptionEventPublisher;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConsultSummaryRetryScheduler {

    private final ConsultSummaryRetryHelper consultSummaryRetryHelper;
    private final TranscriptionEventPublisher transcriptionEventPublisher;
    private final ConsultSummaryService consultSummaryService;

    @Scheduled(fixedRateString = "${consultation-summary.scheduler-fixed-rate}",
            initialDelayString = "${consultation-summary.scheduler-initial-delay}")
    public void processRetry() {
        List<ConsultSummary> consultSummaries = consultSummaryRetryHelper.getFailedConsultSummaries();

        if (consultSummaries.isEmpty()) {
            return;
        }

        log.info("ai 요약 재시도 {}건 처리 시작.", consultSummaries.size());

        consultSummaries.forEach(cs -> {
            try {
                transcriptionEventPublisher.publish(cs.getId(), cs.getAudioLink(), cs.getClinicName(),
                        convertToLocalDateTime(cs.getCreatedAt()).toString());
            } catch (Exception e) {
                log.error("재시도 이벤트 발행 실패. consultSummaryId : [{}], error : [{}]", cs.getId(), e.getMessage());
                try {
                    consultSummaryService.markAsFailed(cs.getId(), "재처리 시도 실패.");
                } catch (Exception markFailedException) {
                    log.error("FAILED 상태 복구 실패. consultSummaryId : [{}], error : [{}]",
                            cs.getId(), markFailedException.getMessage());
                }
            }
        });
    }

    private LocalDateTime convertToLocalDateTime(Instant time) {
        return time.atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

}
