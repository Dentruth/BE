package com.dentruth.consultsummary.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryRetryHelperTest {

    @InjectMocks
    private ConsultSummaryRetryHelper consultSummaryRetryHelper;

    @Mock
    private ConsultSummaryRepository consultSummaryRepository;

    @DisplayName("FAILED 상태인 요약 목록을 조회하고 RETRYING으로 변경한다.")
    @Test
    void shouldReturnFailedSummariesAndChangeStatusToRetrying() {
        //given
        ConsultSummary summary1 = mock(ConsultSummary.class);
        ConsultSummary summary2 = mock(ConsultSummary.class);

        given(consultSummaryRepository.findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED))
                .willReturn(List.of(summary1, summary2));

        //when
        List<ConsultSummary> result = consultSummaryRetryHelper.getFailedConsultSummaries();

        //then
        assertThat(result).hasSize(2);
        verify(summary1, times(1)).changeStatus(SummaryStatus.RETRYING);
        verify(summary2, times(1)).changeStatus(SummaryStatus.RETRYING);
    }

    @DisplayName("FAILED 상태인 요약이 없으면 빈 리스트를 반환하고 상태 변경도 하지 않는다.")
    @Test
    void shouldReturnEmptyList_whenNoFailedSummaries() {
        //given
        given(consultSummaryRepository.findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED))
                .willReturn(List.of());

        //when
        List<ConsultSummary> result = consultSummaryRetryHelper.getFailedConsultSummaries();

        //then
        assertThat(result).isEmpty();
    }

    @DisplayName("FAILED 상태인 요약이 1건이면 해당 건만 RETRYING으로 변경한다.")
    @Test
    void shouldChangeStatusToRetrying_whenOnlyOneFailedSummary() {
        //given
        ConsultSummary summary = mock(ConsultSummary.class);

        given(consultSummaryRepository.findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED))
                .willReturn(List.of(summary));

        //when
        List<ConsultSummary> result = consultSummaryRetryHelper.getFailedConsultSummaries();

        //then
        assertThat(result).hasSize(1);
        verify(summary, times(1)).changeStatus(SummaryStatus.RETRYING);
    }

    @DisplayName("FAILED 이외의 상태는 조회하지 않는다.")
    @Test
    void shouldQueryOnlyFailedStatus() {
        //given
        given(consultSummaryRepository.findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED))
                .willReturn(List.of());

        //when
        consultSummaryRetryHelper.getFailedConsultSummaries();

        //then
        verify(consultSummaryRepository, times(1)).findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED);
        verify(consultSummaryRepository, never()).findAllByStatusAndIsDeletedFalse(SummaryStatus.RETRYING);
        verify(consultSummaryRepository, never()).findAllByStatusAndIsDeletedFalse(SummaryStatus.COMPLETED);
        verify(consultSummaryRepository, never()).findAllByStatusAndIsDeletedFalse(SummaryStatus.PENDING);
    }

}
