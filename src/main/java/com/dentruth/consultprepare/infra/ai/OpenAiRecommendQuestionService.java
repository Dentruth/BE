package com.dentruth.consultprepare.infra.ai;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultprepare.application.RecommendQuestionService;
import com.dentruth.consultprepare.application.dto.response.RecommendQuestionResult;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiRecommendQuestionService
        implements RecommendQuestionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat-model}")
    private String chatModel;

    @Value("${openai.chat-url}")
    private String chatUrl;

    @Value("${openai.prompts.recommend-question}")
    private String promptTemplate;

    @Override
    public RecommendQuestionResult recommendQuestions(
            ConsultPrepare consultPrepare
    ) {

        try {

            log.info(
                    "추천 질문 생성 시작. consultPrepareId={}",
                    consultPrepare.getId()
            );

            String prompt = promptTemplate.formatted(
                    consultPrepare.getPainLocation(),
                    consultPrepare.getPainLevel().name(),
                    consultPrepare.getPainPersistence().name(),
                    consultPrepare.getPainDuration(),
                    consultPrepare.getWorriedIssue(),
                    consultPrepare.getQuestion()
            );

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
                    "temperature", 0.5
            );

            HttpEntity<Map<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            chatUrl,
                            requestEntity,
                            Map.class
                    );

            String aiJsonContent =
                    extractContent(response.getBody());

            log.info("추천 질문 생성 완료.");

            return parseResult(aiJsonContent);

        } catch (Exception e) {

            log.error("추천 질문 생성 실패.", e);

            throw new DentruthException(
                    ErrorStatus.SUMMARIZATION_FAILED
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(
            Map<String, Object> responseBody
    ) {

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseBody.get("choices");

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }

    private RecommendQuestionResult parseResult(
            String aiJsonContent
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(aiJsonContent);

            List<String> questions =
                    new ArrayList<>();

            JsonNode array =
                    root.path("recommendedQuestions");

            for (JsonNode node : array) {
                questions.add(node.asText());
            }

            return RecommendQuestionResult.builder()
                    .recommendedQuestions(questions)
                    .build();

        } catch (Exception e) {

            log.error(
                    "추천 질문 JSON 파싱 실패.",
                    e
            );

            throw new DentruthException(
                    ErrorStatus.SUMMARIZATION_FAILED
            );
        }
    }

}
