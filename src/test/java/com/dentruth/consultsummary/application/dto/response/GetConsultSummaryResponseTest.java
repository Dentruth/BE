package com.dentruth.consultsummary.application.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GetConsultSummaryResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("AI 요약 상태가 COMPLETED라면, JSON 데이터를 파싱하여 바인딩된 Response를 반환한다.")
    @Test
    void shouldReturnFullyParsedResponse_whenStatusIsCompleted() throws Exception {
        //given
        ConsultSummary consultSummary = createBaseConsultSummary();
        ReflectionTestUtils.setField(consultSummary, "status", SummaryStatus.COMPLETED);

        String mockJson = """
                {
                  "diagnosis": {
                    "summary": "치근단 치주염",
                    "summaryEng": "Apical Periodontitis",
                    "description": "치아 뿌리 끝에 염증이 생긴 상태예요.",
                    "descriptionEng": "The nerve inside the tooth is dead."
                  },
                  "treatmentPlan": [
                    {
                      "step": 1,
                      "plan": "오른쪽 아래 첫 번째 큰 어금니(#46) 신경치료",
                      "planEng": "Root canal therapy on the lower right first molar (#46)"
                    }
                  ],
                  "treatmentDelay": [
                    {
                      "title": "잇몸 내 염증 확대",
                      "titleEng": "Expansion of infection within the gums",
                      "description": "방치하면 주변 뼈까지 염증이 퍼져요.",
                      "descriptionEng": "If left untreated, the infection can spread."
                    }
                  ],
                  "treatmentAfterCare": [
                    {
                      "title": "치료 중 음주·흡연",
                      "titleEng": "Drinking or smoking during treatment",
                      "description": "회복이 느려질 수 있어요.",
                      "descriptionEng": "It may slow down recovery."
                    }
                  ]
                }
                """;
        JsonNode rootNode = objectMapper.readTree(mockJson);

        //when
        GetConsultSummaryResponse response = GetConsultSummaryResponse.from(consultSummary, rootNode);

        //then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getFailReason()).isNull();

        assertThat(response.getConsultationInfo().getClinicName()).isEqualTo("강남 치과의원");
        assertThat(response.getConsultationInfo().getDate()).isEqualTo("2026-06-02");

        assertThat(response.getDiagnosis().getSummary()).isEqualTo("치근단 치주염");
        assertThat(response.getDiagnosis().getSummaryEng()).isEqualTo("Apical Periodontitis");
        assertThat(response.getDiagnosis().getDescription()).isEqualTo("치아 뿌리 끝에 염증이 생긴 상태예요.");
        assertThat(response.getDiagnosis().getDescriptionEng()).isEqualTo("The nerve inside the tooth is dead.");

        assertThat(response.getTreatmentPlan()).hasSize(1);
        assertThat(response.getTreatmentPlan().get(0).getStep()).isEqualTo(1);
        assertThat(response.getTreatmentPlan().get(0).getPlan()).isEqualTo("오른쪽 아래 첫 번째 큰 어금니(#46) 신경치료");
        assertThat(response.getTreatmentPlan().get(0).getPlanEng()).isEqualTo("Root canal therapy on the lower right first molar (#46)");

        assertThat(response.getTreatmentDelay()).hasSize(1);
        assertThat(response.getTreatmentDelay().get(0).getTitle()).isEqualTo("잇몸 내 염증 확대");
        assertThat(response.getTreatmentDelay().get(0).getTitleEng()).isEqualTo("Expansion of infection within the gums");

        assertThat(response.getTreatmentAfterCare()).hasSize(1);
        assertThat(response.getTreatmentAfterCare().get(0).getTitle()).isEqualTo("치료 중 음주·흡연");
        assertThat(response.getTreatmentAfterCare().get(0).getDescriptionEng()).isEqualTo("It may slow down recovery.");
    }

    @DisplayName("AI 요약 상태가 COMPLETED가 아니라면, AI 결과물은 매핑하지 않고 최소 기본 정보와 실패 사유만 반환한다.")
    @Test
    void shouldReturnMinimalInfoAndFailReasonWithoutAiResult_whenStatusIsNotCompleted() {
        //given
        ConsultSummary consultSummary = createBaseConsultSummary();
        ReflectionTestUtils.setField(consultSummary, "status", SummaryStatus.FAILED);
        ReflectionTestUtils.setField(consultSummary, "failReason", "[WHISPER_ERR] OpenAI STT Connection Timeout");

        JsonNode emptyNode = objectMapper.createObjectNode();

        //when
        GetConsultSummaryResponse response = GetConsultSummaryResponse.from(consultSummary, emptyNode);

        //then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailReason()).isEqualTo("[WHISPER_ERR] OpenAI STT Connection Timeout");
        assertThat(response.getConsultationInfo().getClinicName()).isEqualTo("강남 치과의원");
        assertThat(response.getDiagnosis()).isNull();
        assertThat(response.getTreatmentPlan()).isNull();
        assertThat(response.getTreatmentDelay()).isNull();
        assertThat(response.getTreatmentAfterCare()).isNull();
    }

    private ConsultSummary createBaseConsultSummary() {
        ConsultSummary summary = ConsultSummary.builder().build();
        ReflectionTestUtils.setField(summary, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(summary, "clinicName", "강남 치과의원");
        ReflectionTestUtils.setField(summary, "createdAt", Instant.parse("2026-06-01T23:00:00Z"));
        return summary;
    }

}
