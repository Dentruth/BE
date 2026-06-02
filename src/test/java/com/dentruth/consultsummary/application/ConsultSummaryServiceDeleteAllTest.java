package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryServiceDeleteAllTest {

    @Mock
    private ConsultSummaryRepository consultSummaryRepository;

    @InjectMocks
    private ConsultSummaryService consultSummaryService;

    private final UUID userId = UUID.randomUUID();

    @DisplayName("요청한 모든 요약본이 존재하고 본인 소유라면, 성공적으로 Soft Delete 된다.")
    @Test
    void deleteAll_success() {
        //given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> summaryIds = List.of(id1, id2);

        ConsultSummary summary1 = createMockSummary(id1, userId, false);
        ConsultSummary summary2 = createMockSummary(id2, userId, false);
        List<ConsultSummary> mockSummaries = List.of(summary1, summary2);

        when(consultSummaryRepository.findAllById(summaryIds)).thenReturn(mockSummaries);

        //when
        consultSummaryService.deleteAll(summaryIds, userId);

        //then
        assertThat(summary1.getIsDeleted()).isTrue();
        assertThat(summary2.getIsDeleted()).isTrue();

        verify(consultSummaryRepository).findAllById(summaryIds);
    }

    @DisplayName("존재하지 않는 id를 포함해 삭제를 요청하면 예외가 발생한다.")
    @Test
    void deleteAll_sizeMismatch_throwsException() {
        //given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> summaryIds = List.of(id1, id2);

        ConsultSummary summary1 = createMockSummary(id1, userId, false);
        List<ConsultSummary> mockSummaries = List.of(summary1);

        when(consultSummaryRepository.findAllById(summaryIds)).thenReturn(mockSummaries);

        //when, then
        assertThatThrownBy(() -> consultSummaryService.deleteAll(summaryIds, userId))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getMessage());

        verify(consultSummaryRepository).findAllById(summaryIds);
    }

    private ConsultSummary createMockSummary(UUID id, UUID userId, boolean isDeleted) {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", id);
        ReflectionTestUtils.setField(summary, "userId", userId);
        ReflectionTestUtils.setField(summary, "isDeleted", isDeleted);
        return summary;
    }

}
