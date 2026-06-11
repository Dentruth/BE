package com.dentruth.consultsummary.infra.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dentruth.consultsummary.application.ConsultSummaryService;
import com.dentruth.consultsummary.application.TranscriptionEventPublisher;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryRetrySchedulerTest {

    @InjectMocks
    private ConsultSummaryRetryScheduler consultSummaryRetryScheduler;

    @Mock
    private ConsultSummaryRetryHelper consultSummaryRetryHelper;

    @Mock
    private TranscriptionEventPublisher transcriptionEventPublisher;

    @Mock
    private ConsultSummaryService consultSummaryService;

    @DisplayName("재시도 대상이 없으면 이벤트를 발행하지 않는다.")
    @Test
    void shouldNotPublishEvent_whenNoFailedSummaries() {
        //given
        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of());

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(transcriptionEventPublisher, never()).publish(any(), any(), any(), any());
        verify(consultSummaryService, never()).markAsFailed(any(), any());
    }

    @DisplayName("재시도 대상이 있으면 각 건마다 이벤트를 발행한다.")
    @Test
    void shouldPublishEvent_forEachFailedSummary() {
        //given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ConsultSummary summary1 = createMockSummary(id1, "강남치과");
        ConsultSummary summary2 = createMockSummary(id2, "서초치과");

        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of(summary1, summary2));

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(transcriptionEventPublisher, times(2)).publish(any(), any(), any(), any());
        verify(transcriptionEventPublisher, times(1)).publish(eq(id1), any(), any(), any());
        verify(transcriptionEventPublisher, times(1)).publish(eq(id2), any(), any(), any());
    }

    @DisplayName("이벤트 발행 성공 시 FAILED 상태로 되돌리지 않는다.")
    @Test
    void shouldNotMarkAsFailed_whenEventPublishSucceeds() {
        //given
        ConsultSummary summary = createMockSummary(UUID.randomUUID(), "강남치과");
        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of(summary));

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(consultSummaryService, never()).markAsFailed(any(), any());
    }

    @DisplayName("이벤트 발행 실패 시 해당 건을 FAILED 상태로 되돌린다.")
    @Test
    void shouldMarkAsFailed_whenEventPublishFails() {
        //given
        UUID summaryId = UUID.randomUUID();
        ConsultSummary summary = createMockSummary(summaryId, "강남치과");

        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of(summary));
        willThrow(new RuntimeException("Redis 연결 실패")).given(transcriptionEventPublisher)
                .publish(any(), any(), any(), any());

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(consultSummaryService, times(1)).markAsFailed(eq(summaryId), any());
    }

    @DisplayName("이벤트 발행 중 하나가 실패해도 나머지는 계속 발행된다.")
    @Test
    void shouldContinuePublishing_whenOneEventFails() {
        //given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        ConsultSummary summary1 = createMockSummary(id1, "강남치과");
        ConsultSummary summary2 = createMockSummary(id2, "서초치과");
        ConsultSummary summary3 = createMockSummary(id3, "송파치과");

        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of(summary1, summary2, summary3));

        lenient().doAnswer(invocation -> {
            UUID summaryId = invocation.getArgument(0);
            if (summaryId.equals(id2)) {
                throw new RuntimeException("Redis 연결 실패");
            }
            return null;
        }).when(transcriptionEventPublisher).publish(any(), any(), any(), any());

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(transcriptionEventPublisher, times(1)).publish(eq(id1), any(), any(), any());
        verify(transcriptionEventPublisher, times(1)).publish(eq(id2), any(), any(), any());
        verify(transcriptionEventPublisher, times(1)).publish(eq(id3), any(), any(), any()); // 계속 진행
        verify(consultSummaryService, times(1)).markAsFailed(eq(id2), any()); // 실패한 건만 복구
        verify(consultSummaryService, never()).markAsFailed(eq(id1), any());
        verify(consultSummaryService, never()).markAsFailed(eq(id3), any());
    }

    @DisplayName("이벤트 발행 실패 시 FAILED 복구(markAsFailed)도 실패하면 예외 없이 계속 진행된다.")
    @Test
    void shouldContinueWithoutException_whenMarkAsFailedAlsoFails() {
        //given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ConsultSummary summary1 = createMockSummary(id1, "강남치과");
        ConsultSummary summary2 = createMockSummary(id2, "서초치과");

        given(consultSummaryRetryHelper.getFailedConsultSummaries()).willReturn(List.of(summary1, summary2));

        willThrow(new RuntimeException("Redis 연결 실패")).given(transcriptionEventPublisher)
                .publish(eq(id1), any(), any(), any());

        willThrow(new RuntimeException("DB 연결 실패")).given(consultSummaryService).markAsFailed(eq(id1), any());

        //when
        consultSummaryRetryScheduler.processRetry();

        //then
        verify(transcriptionEventPublisher, times(1)).publish(eq(id2), any(), any(), any());
    }

    private ConsultSummary createMockSummary(UUID id, String clinicName) {
        ConsultSummary summary = mock(ConsultSummary.class);
        lenient().when(summary.getId()).thenReturn(id);
        lenient().when(summary.getAudioLink()).thenReturn("s3://bucket/audio/" + id + ".mp3");
        lenient().when(summary.getClinicName()).thenReturn(clinicName);
        lenient().when(summary.getCreatedAt()).thenReturn(Instant.now());
        return summary;
    }

}
