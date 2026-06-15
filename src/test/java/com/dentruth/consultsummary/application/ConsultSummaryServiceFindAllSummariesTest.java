package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryServiceFindAllSummariesTest {

    @InjectMocks
    private ConsultSummaryService consultSummaryService;

    @Mock
    private ConsultSummaryRepository consultSummaryRepository;

    @Nested
    @DisplayName("상담 요약 목록 페이징 조회 테스트")
    class FindAllSummariesTest {

        private final UUID userId = UUID.randomUUID();
        private final int size = 10;
        private final PageRequest expectedPageRequest = PageRequest.of(0, size + 1);

        @DisplayName("커서(cursor)가 null이면 첫 페이지 조회 쿼리를 호출하고 결과를 반환한다.")
        @Test
        void findAllSummaries_cursorNull_returnsFirstPage() {
            //given
            List<ConsultSummary> mockResult = List.of(createMockSummary(UUID.randomUUID(), Instant.now()));
            when(consultSummaryRepository.findFirstPage(userId, expectedPageRequest))
                    .thenReturn(mockResult);

            //when
            List<ConsultSummary> result = consultSummaryService.findAllSummaries(userId, null, size);

            //then
            assertThat(result).hasSize(1);
            assertThat(result).isEqualTo(mockResult);
            verify(consultSummaryRepository).findFirstPage(userId, expectedPageRequest);
        }

        @DisplayName("커서(cursor)가 존재할 때, 커서에 해당하는 요약 정보가 존재하면 다음 페이지 조회 쿼리를 호출한다.")
        @Test
        void findAllSummaries_cursorExists_returnsNextPage() {
            //given
            UUID cursorId = UUID.randomUUID();
            Instant cursorCreatedAt = Instant.parse("2026-06-02T10:00:00Z");
            ConsultSummary cursorSummary = createMockSummary(cursorId, cursorCreatedAt);

            List<ConsultSummary> mockResult = List.of(createMockSummary(UUID.randomUUID(), Instant.now()));

            when(consultSummaryRepository.findById(cursorId)).thenReturn(Optional.of(cursorSummary));
            when(consultSummaryRepository.findNextPage(userId, cursorCreatedAt, cursorId, expectedPageRequest))
                    .thenReturn(mockResult);

            //when
            List<ConsultSummary> result = consultSummaryService.findAllSummaries(userId, cursorId, size);

            //then
            assertThat(result).hasSize(1);
            assertThat(result).isEqualTo(mockResult);
            verify(consultSummaryRepository).findById(cursorId);
            verify(consultSummaryRepository).findNextPage(userId, cursorCreatedAt, cursorId, expectedPageRequest);
        }

        @DisplayName("커서(cursor)에 해당하는 요약 정보가 DB에 존재하지 않으면 SUMMARY_RECORD_NOT_FOUND 예외를 던진다.")
        @Test
        void findAllSummaries_cursorNotFound_throwsException() {
            //given
            UUID invalidCursorId = UUID.randomUUID();
            when(consultSummaryRepository.findById(invalidCursorId)).thenReturn(Optional.empty());

            //when, then
            assertThatThrownBy(() -> consultSummaryService.findAllSummaries(userId, invalidCursorId, size))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getMessage());

            verify(consultSummaryRepository).findById(invalidCursorId);
        }
    }

    private ConsultSummary createMockSummary(UUID id, Instant createdAt) {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", id);
        ReflectionTestUtils.setField(summary, "createdAt", createdAt);
        return summary;
    }

}
