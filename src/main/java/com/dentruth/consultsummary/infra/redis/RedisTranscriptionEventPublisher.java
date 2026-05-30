package com.dentruth.consultsummary.infra.redis;

import com.dentruth.consultsummary.application.TranscriptionEventPublisher;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTranscriptionEventPublisher implements TranscriptionEventPublisher {

    private final StringRedisTemplate redisTemplate;

    public static final String STREAM_KEY = "consult-summary:transcription";

    @Override
    public void publish(UUID summaryId, String s3Key, String clinicName, String createdAt) {
        Map<String, String> message = Map.of(
                "summaryId", summaryId.toString(),
                "s3Key", s3Key,
                "clinicName", clinicName,
                "createdAt", createdAt
        );

        RecordId recordId = redisTemplate.opsForStream().add(MapRecord.create(STREAM_KEY, message));

        log.info("Redis Streams 이벤트 발행 완료. summaryId : [{}], recordId : [{}]", summaryId, recordId);
    }

}
