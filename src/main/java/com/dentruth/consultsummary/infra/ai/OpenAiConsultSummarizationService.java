package com.dentruth.consultsummary.infra.ai;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultsummary.application.ConsultSummarizationService;
import com.dentruth.consultsummary.application.dto.SummarizedResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiConsultSummarizationService implements ConsultSummarizationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat-model}")
    private String chatModel;

    @Value("${openai.chat-url}")
    private String chatUrl;

    @Value("${openai.prompts.consultation-summary}")
    private String promptTemplate;

    @Override
    public SummarizedResult summarize(String transcribedText, String clinicName, String date) {
        try {
            String prompt = promptTemplate.formatted(clinicName, date, transcribedText);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", chatModel,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.2
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(chatUrl, requestEntity, Map.class);

            String aiJsonContent = extractContent(response.getBody());
            log.info("Open AI 요약 응답 수신.");

            return parseToSummarizedResult(aiJsonContent);
        } catch (Exception e) {
            log.error("Open AI 요약 실패.", e);
            throw new DentruthException(ErrorStatus.SUMMARIZATION_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private SummarizedResult parseToSummarizedResult(String aiJsonContent) {
        try {
            JsonNode root = objectMapper.readTree(aiJsonContent);

            String diagnosis = root.path("diagnosticResults").asText("");
            String title = root.path("title").asText("");
            String treatmentSummary = root.path("treatmentSummary").asText("");

            return SummarizedResult.builder()
                    .rawJson(aiJsonContent)
                    .diagnosis(diagnosis)
                    .title(title)
                    .treatmentPlan(treatmentSummary)
                    .build();
        } catch (Exception e) {
            log.error("Open AI 응답 Json 파싱 실패.", e);
            throw new DentruthException(ErrorStatus.SUMMARIZATION_FAILED);
        }
    }

}
