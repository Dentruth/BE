package com.dentruth.consultprepare.infra.ai;

import com.dentruth.consultprepare.application.ConsultTranslationService;
import com.dentruth.consultprepare.application.dto.response.ConsultTranslationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiConsultTranslationService implements ConsultTranslationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat-model}")
    private String chatModel;

    @Value("${openai.chat-url}")
    private String chatUrl;

    @Value("${openai.prompts.consult-translation}")
    private String promptTemplate;

    @Override
    public ConsultTranslationResult translate(
            String painOrigin,
            String worriedIssue,
            String question
    ) {
        try {

            log.info(
                    "OpenAI 상담카드 번역 요청 시작. painOrigin={}, worriedIssue={}, question={}",
                    painOrigin,
                    worriedIssue,
                    question
            );

            String prompt = promptTemplate.formatted(
                    painOrigin,
                    worriedIssue,
                    question
            );

            log.debug("OpenAI Prompt=\n{}", prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", chatModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.2
            );

            HttpEntity<Map<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            log.info(
                    "OpenAI API 호출. model={}",
                    chatModel
            );

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            chatUrl,
                            requestEntity,
                            Map.class
                    );

            String aiJsonContent = extractContent(response.getBody());

            log.info(
                    "OpenAI 상담카드 번역 응답 수신."
            );

            ConsultTranslationResult result = parseResult(aiJsonContent);

            log.info(
                    "OpenAI 응답 파싱 완료. painOrigin={}, painLocationKo={}",
                    result.getPainOrigin(),
                    result.getPainLocationKo()
            );

            return result;


        } catch (Exception e) {

            log.error("OpenAI 상담카드 번역 실패.", e);

            throw new DentruthException(
                    ErrorStatus.TRANSFER_FAILED
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseBody.get("choices");

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }

    private ConsultTranslationResult parseResult(String aiJsonContent) {

        try {

            JsonNode root = objectMapper.readTree(aiJsonContent);

            return ConsultTranslationResult.builder()
                    .painOrigin(root.path("painOrigin").asText(""))
                    .painKo(root.path("painKo").asText(""))
                    .painLocationKo(root.path("painLocationKo").asText(""))
                    .visitPurpose(root.path("visitPurpose").asText(""))
                    .build();

        } catch (Exception e) {

            log.error("OpenAI 응답 Json 파싱 실패.", e);

            throw new DentruthException(
                    ErrorStatus.OPENAI_REQUEST_FAILED
            );
        }
    }

}
