package com.dentruth.consultsummary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import com.dentruth.consultsummary.infra.ai.OpenAiConsultSummarizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ConsultSummarizationServiceTest {

    @InjectMocks
    private OpenAiConsultSummarizationService openAiConsultSummarizationService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final String chatUrl = "https://api.openai.com/v1/chat/completions";
    private final String transcribedText = "치아가 너무 아파요. 크라운 치료가 필요합니다.";
    private final String clinicName = "강남 병원";
    private final String date = "2026-05-28";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openAiConsultSummarizationService, "apiKey", "mock-api-key");
        ReflectionTestUtils.setField(openAiConsultSummarizationService, "chatModel", "gpt-4o");
        ReflectionTestUtils.setField(openAiConsultSummarizationService, "chatUrl", chatUrl);
        ReflectionTestUtils.setField(openAiConsultSummarizationService, "promptTemplate", "병원명: %s, 날짜: %s, 내용: %s");
    }

    @DisplayName("STT 텍스트 전문과 병원 이름, 날짜가 유효하면 Open AI 요약에 성공한다.")
    @Test
    void shouldSummarizeConsultationSuccessfully_whenSttTextAndHospitalNameAndDateAreValid() {
        //given
        String mockAiJsonContent = """
                {
                  "diagnosticResults": "급성 치수염",
                  "treatmentSummary": "크라운 치료",
                  "title" : "치통으로 인한 크라운 치료 요약"
                }
                """;

        Map<String, Object> mockMessage = Map.of("role", "assistant", "content", mockAiJsonContent);
        Map<String, Object> mockChoice = Map.of("message", mockMessage);
        Map<String, Object> mockResponseBody = Map.of("choices", List.of(mockChoice));

        ResponseEntity<Map> responseEntity = ResponseEntity.ok(mockResponseBody);

        given(restTemplate.postForEntity(eq(chatUrl), any(HttpEntity.class), eq(Map.class))).willReturn(responseEntity);

        //when
        SummarizedResult result = openAiConsultSummarizationService.summarize(transcribedText, clinicName, date);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getDiagnosis()).isEqualTo("급성 치수염");
        assertThat(result.getTitle()).isEqualTo("치통으로 인한 크라운 치료 요약");
        assertThat(result.getTreatmentPlan()).isEqualTo("크라운 치료");
        assertThat(result.getRawJson()).isEqualTo(mockAiJsonContent);
    }

    @DisplayName("Open AI API 호출 중 예외가 발생하면 예외를 던진다.")
    @Test
    void shouldThrowException_whenOpenAiApiFails() {
        //given
        given(restTemplate.postForEntity(eq(chatUrl), any(HttpEntity.class), eq(Map.class))).willThrow(
                new RuntimeException("OpenAI API 타임아웃."));

        //when, then
        assertThatThrownBy(()->openAiConsultSummarizationService.summarize(transcribedText, clinicName, date))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUMMARIZATION_FAILED.getMessage());
    }

    @DisplayName("Open AI API 호출에는 성공했지만, 반환된 json 포맷이 유효하지 않으면 예외를 던진다.")
    @Test
    void shouldThrowException_whenAiResponseJsonIsMalformed() {
        //given
        String mockAiJsonContent = """
                {
                  "diagnosticResults":  "title": "급성 치수염" ,
                  "treatmentPlan": "신경치료 후 보철(크라운) 수복 진행",
                  "treatmentSummary": "치통으로 인한 크라운 치료 요약"
                """;

        Map<String, Object> mockMessage = Map.of("role", "assistant", "content", mockAiJsonContent);
        Map<String, Object> mockChoice = Map.of("message", mockMessage);
        Map<String, Object> mockResponseBody = Map.of("choices", List.of(mockChoice));

        ResponseEntity<Map> responseEntity = ResponseEntity.ok(mockResponseBody);

        given(restTemplate.postForEntity(eq(chatUrl), any(HttpEntity.class), eq(Map.class))).willReturn(responseEntity);

        //when, then
        assertThatThrownBy(()->openAiConsultSummarizationService.summarize(transcribedText, clinicName, date))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUMMARIZATION_FAILED.getMessage());
    }

}
