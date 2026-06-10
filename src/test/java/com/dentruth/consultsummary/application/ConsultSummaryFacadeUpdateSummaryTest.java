package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dentruth.consultsummary.application.dto.request.UpdateSummaryApplicationRequest;
import com.dentruth.consultsummary.application.dto.response.GetConsultSummaryResponse;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryFacadeUpdateSummaryTest {

    @Mock
    private ConsultSummaryService consultSummaryService;

    @InjectMocks
    @Spy
    private ConsultSummaryFacade consultSummaryFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID summaryId = UUID.randomUUID();

    @DisplayName("요약 카드를 정상 수정하고 상태가 COMPLETED라면, JSON 문자열을 파싱하여 다국어 스펙 DTO를 반환한다.")
    @Test
    void updateSummary_statusCompleted_success() throws Exception {
        //given
        UpdateSummaryApplicationRequest request = UpdateSummaryApplicationRequest.builder()
                .clinicName("강남 세브란스 치과")
                .build();

        String mockDiagnosticResult = """
                    {
                      "diagnosis": {
                        "summary": "치근단 치주염",
                        "summaryEng": "Apical Periodontitis",
                        "description": "치아 뿌리 끝에 염증이 생긴 상태예요.",
                        "descriptionEng": "Infection at the root end."
                      }
                    }
                    """;

        ConsultSummary mockUpdatedSummary = createMockSummary(summaryId, userId, "강남 세브란스 치과", mockDiagnosticResult, SummaryStatus.COMPLETED);

        doNothing().when(consultSummaryFacade).findUser(userId, "ai 녹음 요약 수정 요청");
        when(consultSummaryService.update(userId, summaryId, request)).thenReturn(mockUpdatedSummary);

        ReflectionTestUtils.setField(consultSummaryFacade, "objectMapper", objectMapper);

        //when
        GetConsultSummaryResponse response = consultSummaryFacade.updateSummary(userId, summaryId, request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getConsultationInfo().getClinicName()).isEqualTo("강남 세브란스 치과");

        assertThat(response.getDiagnosis().getSummary()).isEqualTo("치근단 치주염");
        assertThat(response.getDiagnosis().getSummaryEng()).isEqualTo("Apical Periodontitis");

        verify(consultSummaryFacade).findUser(userId, "ai 녹음 요약 수정 요청");
        verify(consultSummaryService).update(userId, summaryId, request);
    }

    @DisplayName("요약 카드를 수정했으나 상태가 COMPLETED가 아니라면, 데이터 파싱을 건너뛰고 최소 정보만 담아 반환한다.")
    @Test
    void updateSummary_statusNotCompleted_returnsIncompleteResponse() {
        //given
        UpdateSummaryApplicationRequest request = UpdateSummaryApplicationRequest.builder()
                .clinicName("대구 치과")
                .build();

        ConsultSummary mockUpdatedSummary = createMockSummary(summaryId, userId, "대구 치과", null, SummaryStatus.FAILED);
        ReflectionTestUtils.setField(mockUpdatedSummary, "failReason", "STT 타임아웃 장애");

        doNothing().when(consultSummaryFacade).findUser(userId, "ai 녹음 요약 수정 요청");
        when(consultSummaryService.update(userId, summaryId, request)).thenReturn(mockUpdatedSummary);

        //when
        GetConsultSummaryResponse response = consultSummaryFacade.updateSummary(userId, summaryId, request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailReason()).isEqualTo("STT 타임아웃 장애");

        assertThat(response.getDiagnosis()).isNull();
        assertThat(response.getTreatmentPlan()).isNull();
        verify(consultSummaryFacade).findUser(userId, "ai 녹음 요약 수정 요청");
        verify(consultSummaryService).update(userId, summaryId, request);
    }

    private ConsultSummary createMockSummary(UUID id, UUID userId, String clinicName, String diagnosticResult, SummaryStatus status) {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", id);
        ReflectionTestUtils.setField(summary, "userId", userId);
        ReflectionTestUtils.setField(summary, "clinicName", clinicName);
        ReflectionTestUtils.setField(summary, "diagnosticResult", diagnosticResult);
        ReflectionTestUtils.setField(summary, "status", status);
        ReflectionTestUtils.setField(summary, "createdAt", Instant.now());
        return summary;
    }

}
