package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.request.UpdateSummaryApplicationRequest;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryServiceUpdateTest {

    @Mock
    private ConsultSummaryRepository consultSummaryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConsultSummaryService consultSummaryService;

    private final UUID userId = UUID.randomUUID();
    private final UUID summaryId = UUID.randomUUID();

    @DisplayName("수정 요청 데이터가 올바르고 본인 소유의 삭제되지 않은 카드라면, 성공적으로 값이 변경되고 엔티티를 반환한다.")
    @Test
    void update_success() {
        //given
        UpdateSummaryApplicationRequest request = UpdateSummaryApplicationRequest.builder()
                .clinicName("수정된 치과의원")
                .diagnosis(UpdateSummaryApplicationRequest.Diagnosis.builder()
                        .summary("치수염")
                        .build())
                .build();

        ConsultSummary mockSummary = createMockSummary(summaryId, userId, "기존 치과", "기존 진단", false);
        String mockSerializedJson = "{\"clinicName\":\"수정된 치과의원\",\"diagnosis\":{\"summary\":\"치수염\",\"summaryEng\":null,\"description\":null,\"descriptionEng\":null},\"treatmentPlan\":null,\"treatmentDelay\":null,\"treatmentAfterCare\":null}";

        given(consultSummaryRepository.findById(summaryId)).willReturn(Optional.of(mockSummary));

        //when
        ConsultSummary result = consultSummaryService.update(userId, summaryId, request);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getClinicName()).isEqualTo("수정된 치과의원");
        assertThat(result.getDiagnosis()).isEqualTo("치수염");
        assertThat(result.getDiagnosticResult()).isEqualTo(mockSerializedJson);

        verify(consultSummaryRepository).findById(summaryId);
    }

    @DisplayName("수정하려는 요약 내역이 이미 삭제된 상태라면 예외가 발생한다..")
    @Test
    void update_alreadyDeleted_throwsException() {
        //given
        UpdateSummaryApplicationRequest request = UpdateSummaryApplicationRequest.builder().build();
        ConsultSummary mockSummary = createMockSummary(summaryId, userId, "기존 치과", "기존 진단", true);

        given(consultSummaryRepository.findById(summaryId)).willReturn(Optional.of(mockSummary));

        //when, then
        assertThatThrownBy(() -> consultSummaryService.update(userId, summaryId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUMMARY_RECORD_NOT_FOUND.getMessage());

        verify(consultSummaryRepository).findById(summaryId);
    }

    @DisplayName("수정하려는 요약 내역의 소유자가 요청한 유저와 다르면 예외가 발생한다..")
    @Test
    void update_notOwner_throwsException() {
        //given
        UpdateSummaryApplicationRequest request = UpdateSummaryApplicationRequest.builder().build();
        UUID anotherUserId = UUID.randomUUID();
        ConsultSummary mockSummary = createMockSummary(summaryId, anotherUserId, "기존 치과", "기존 진단", false);

        given(consultSummaryRepository.findById(summaryId)).willReturn(Optional.of(mockSummary));

        //when, then
        assertThatThrownBy(() -> consultSummaryService.update(userId, summaryId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.FORBIDDEN.getMessage());

        verify(consultSummaryRepository).findById(summaryId);
    }

    private ConsultSummary createMockSummary(UUID id, UUID userId, String clinicName, String diagnosis,
                                             boolean isDeleted) {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", id);
        ReflectionTestUtils.setField(summary, "userId", userId);
        ReflectionTestUtils.setField(summary, "clinicName", clinicName);
        ReflectionTestUtils.setField(summary, "diagnosis", diagnosis);
        ReflectionTestUtils.setField(summary, "isDeleted", isDeleted);
        ReflectionTestUtils.setField(summary, "status", SummaryStatus.COMPLETED);
        return summary;
    }
}
