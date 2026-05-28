package com.dentruth.consultsummary.infra.redis;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.AudioTranscriptionService;
import com.dentruth.consultsummary.application.ConsultSummarizationService;
import com.dentruth.consultsummary.application.ConsultSummaryService;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionStreamConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final AudioTranscriptionService audioTranscriptionService;
    private final ConsultSummaryService consultSummaryService;
    private final ConsultSummarizationService consultSummarizationService;

    private static final String STREAM_KEY = RedisTranscriptionEventPublisher.STREAM_KEY;
    private static final String GROUP_NAME = "transcription-group";
    private static final String CONSUMER_NAME = "consumer-1";
    private static final int BATCH_SIZE = 3;

    @PostConstruct
    public void initConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("Consumer group 생성 완료. group : [{}]", GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group 이미 존재하거나 스트림 미생성. 계속 진행.");
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_SIZE),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );
        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            processRecord(record);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        UUID summaryId = UUID.fromString(record.getValue().get("summaryId").toString());
        String s3Key = record.getValue().get("s3Key").toString();
        String clinicName = record.getValue().get("clinicName").toString();
        String createdAt = record.getValue().get("createdAt").toString();

        log.info("STT + 요약 처리 시작. summaryId : [{}]", summaryId);

        try {
            String transcribedText = audioTranscriptionService.transcribe(s3Key);
            log.info("STT 처리 완료. summaryId : [{}]", summaryId);

            SummarizedResult result = consultSummarizationService.summarize(transcribedText, clinicName, createdAt);
            log.info("Open AI 요약 처리 완료. summaryId : [{}]", summaryId);

            consultSummaryService.markAsCompleted(summaryId, result);

            stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
            log.info("STT + 요약 처리 완료. summaryId : [{}]", summaryId);
        } catch (Exception e) {
            log.error("STT + 요약 처리 실패. summaryId : [{}]", summaryId, e);
            String failReason = ErrorStatus.INTERNAL_SERVER_ERROR.getCode();

            if (e instanceof DentruthException dentruthException) {
                failReason = dentruthException.getMessage();
            }

            consultSummaryService.markAsFailed(summaryId, failReason);
        }
    }

}
