package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.dentruth.consultsummary.application.dto.response.GetConsultSummaryResponse;
import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.user.application.UserService;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultSummaryFacadeGetDetailTest {

    @InjectMocks
    private ConsultSummaryFacade consultSummaryFacade;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserService userService;

    @Mock
    private ConsultSummaryService consultSummaryService;

    @Mock
    private TranscriptionEventPublisher transcriptionEventPublisher;

    @Mock
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final static String mockJson = """
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

    @DisplayName("유저가 존재하고, 요약 정보가 존재하며 요약 상태가 COMPLETED라면 올바른 요약 정보를 반환한다.")
    @Test
    void shouldReturnCorrectConsultSummary_whenUserAndSummaryExistWithCompletedStatus() {
        //given
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .build();
        given(userService.findById(userId, "ai 요약 내용 조회")).willReturn(mockUser);

        UUID consultSummaryId = UUID.randomUUID();
        ConsultSummary mockConsultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .isDeleted(false)
                .audioLink("audioLink")
                .clinicName("강남 치과의원")
                .status(SummaryStatus.COMPLETED)
                .diagnosticResult(mockJson)
                .build();
        ReflectionTestUtils.setField(mockConsultSummary, "createdAt", Instant.parse("2026-06-01T23:00:00Z"));
        given(consultSummaryService.findById(consultSummaryId, userId)).willReturn(mockConsultSummary);

        //when
        GetConsultSummaryResponse response = consultSummaryFacade.getDetail(userId, consultSummaryId);

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
        assertThat(response.getTreatmentPlan().get(0).getPlanEng()).isEqualTo(
                "Root canal therapy on the lower right first molar (#46)");

        assertThat(response.getTreatmentDelay()).hasSize(1);
        assertThat(response.getTreatmentDelay().get(0).getTitle()).isEqualTo("잇몸 내 염증 확대");
        assertThat(response.getTreatmentDelay().get(0).getTitleEng()).isEqualTo(
                "Expansion of infection within the gums");

        assertThat(response.getTreatmentAfterCare()).hasSize(1);
        assertThat(response.getTreatmentAfterCare().get(0).getTitle()).isEqualTo("치료 중 음주·흡연");
        assertThat(response.getTreatmentAfterCare().get(0).getDescriptionEng()).isEqualTo("It may slow down recovery.");
    }

    @DisplayName("AI 요약 상태가 COMPLETED가 아니라면, AI 결과물은 매핑하지 않고 최소 기본 정보와 실패 사유만 반환한다.")
    @ParameterizedTest(name = "[{index}] 요약 상태 : {0}")
    @EnumSource(value = SummaryStatus.class, names = {"UPLOADING", "PENDING", "ANALYZING", "FAILED", "RETRYING"})
    void shouldReturnMinimalInfoAndFailReasonWithoutAiResult_whenStatusIsNotCompleted(SummaryStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .build();
        given(userService.findById(userId, "ai 요약 내용 조회")).willReturn(mockUser);

        UUID consultSummaryId = UUID.randomUUID();
        ConsultSummary mockConsultSummary = ConsultSummary.builder()
                .id(consultSummaryId)
                .isDeleted(false)
                .audioLink("audioLink")
                .clinicName("강남 치과의원")
                .status(status)
                .failReason("[WHISPER_ERR] OpenAI STT Connection Timeout")
                .diagnosticResult(mockJson)
                .build();
        ReflectionTestUtils.setField(mockConsultSummary, "createdAt", Instant.parse("2026-06-01T23:00:00Z"));
        given(consultSummaryService.findById(consultSummaryId, userId)).willReturn(mockConsultSummary);

        //when
        GetConsultSummaryResponse response = consultSummaryFacade.getDetail(userId, consultSummaryId);

        //then
        assertThat(response.getStatus()).isEqualTo(status.name());
        assertThat(response.getFailReason()).isEqualTo("[WHISPER_ERR] OpenAI STT Connection Timeout");
        assertThat(response.getConsultationInfo().getClinicName()).isEqualTo("강남 치과의원");
        assertThat(response.getDiagnosis()).isNull();
        assertThat(response.getTreatmentPlan()).isNull();
        assertThat(response.getTreatmentDelay()).isNull();
        assertThat(response.getTreatmentAfterCare()).isNull();
    }

}
