package com.dentruth.consultsummary.infra.redis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dentruth.consultsummary.application.AudioTranscriptionService;
import com.dentruth.consultsummary.application.ConsultSummarizationService;
import com.dentruth.consultsummary.application.ConsultSummaryService;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class TranscriptionStreamConsumerTest {

    @InjectMocks
    private TranscriptionStreamConsumer transcriptionStreamConsumer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private AudioTranscriptionService audioTranscriptionService;

    @Mock
    private ConsultSummaryService consultSummaryService;

    @Mock
    private ConsultSummarizationService consultSummarizationService;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private final UUID summaryId = UUID.randomUUID();
    private final String s3Key = "audio/test.mp3";
    private final String clinicName = "강남 병원";
    private final String createdAt = "2026-05-28T21:00:00";

    @DisplayName("Redis 스트림에 레코드가 존재하면 STT 변환 및 AI 요약에 성공한다.")
    @Test
    void shouldProcessRecordAndAck_whenStreamRecordExists() {
        //given
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);

        Map<Object, Object> streamFields = Map.of("summaryId", summaryId.toString(), "s3Key", s3Key, "clinicName",
                clinicName, "createdAt", createdAt);

        String streamKey = RedisTranscriptionEventPublisher.STREAM_KEY;

        RecordId mockRecordId = RecordId.of("1716900000000-0");
        MapRecord<String, Object, Object> mockRecord = MapRecord.create("transcription-stream", streamFields)
                .withId(mockRecordId);

        given(streamOperations.read(any(Consumer.class), any(StreamReadOptions.class),
                any(StreamOffset.class))).willReturn(List.of(mockRecord));

        String expectedTranscribedText = "치과 진료 내용입니다.";
        given(audioTranscriptionService.transcribe(s3Key)).willReturn(expectedTranscribedText);

        SummarizedResult expectedResult = mock(SummarizedResult.class);
        given(consultSummarizationService.summarize(expectedTranscribedText, clinicName, createdAt))
                .willReturn(expectedResult);

        //when
        transcriptionStreamConsumer.consume();

        //then
        verify(consultSummaryService).markAsCompleted(summaryId, expectedResult);
        verify(streamOperations).acknowledge(streamKey, "transcription-group", mockRecordId);
    }

    @DisplayName("비즈니스 로직 처리 중 에러가 발생하면 실패 상태로 변경하고 ACK를 보내지 않는다.")
    @Test
    void shouldMarkAsFailedAndNotAck_whenExceptionOccursDuringProcessing() {
        //given
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);

        Map<Object, Object> streamFields = Map.of(
                "summaryId", summaryId.toString(),
                "s3Key", s3Key,
                "clinicName", clinicName,
                "createdAt", createdAt
        );
        RecordId mockRecordId = RecordId.of("1716900000000-0");
        MapRecord<String, Object, Object> mockRecord = MapRecord.create("transcription-stream", streamFields)
                .withId(mockRecordId);

        given(streamOperations.read(any(Consumer.class), any(StreamReadOptions.class),
                any(StreamOffset.class))).willReturn(List.of(mockRecord));
        given(audioTranscriptionService.transcribe(s3Key))
                .willThrow(new RuntimeException("AI 인프라 일시적 장애"));

        //when
        transcriptionStreamConsumer.consume();

        //then
        verify(consultSummaryService).markAsFailed(summaryId, "COMMON_500");
        verify(streamOperations, never()).acknowledge(any(String.class), any(String.class), any(RecordId.class));
    }

}
