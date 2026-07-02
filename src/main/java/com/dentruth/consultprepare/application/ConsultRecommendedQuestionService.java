package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.response.RecommendQuestionResult;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.entity.ConsultRecommendedQuestion;
import com.dentruth.consultprepare.domain.repository.ConsultRecommendedQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultRecommendedQuestionService {

    private final RecommendQuestionService recommendQuestionService;
    private final ConsultRecommendedQuestionRepository consultRecommendedQuestionRepository;

    @Transactional
    public List<String> generateIfAbsent(
            ConsultPrepare consultPrepare
    ) {

        Long consultCardId = consultPrepare.getId();

        List<ConsultRecommendedQuestion> existing =
                consultRecommendedQuestionRepository
                        .findAllByConsultPrepareIdOrderByQuestionOrderAsc(
                                consultCardId
                        );

        if (!existing.isEmpty()) {
            return toQuestionTexts(existing);
        }

        log.info(
                "추천 질문이 존재하지 않습니다. 생성 시작. consultCardId={}",
                consultCardId
        );

        return generateAndSave(consultCardId, consultPrepare);
    }

    @Transactional
    public List<String> regenerate(
            ConsultPrepare consultPrepare
    ) {

        Long consultCardId = consultPrepare.getId();

        consultRecommendedQuestionRepository
                .deleteAllByConsultPrepareId(consultCardId);

        log.info(
                "추천 질문 재생성 시작. consultCardId={}",
                consultCardId
        );

        return generateAndSave(consultCardId, consultPrepare);
    }

    private List<String> generateAndSave(
            Long consultCardId,
            ConsultPrepare consultPrepare
    ) {

        RecommendQuestionResult result =
                recommendQuestionService.recommendQuestions(consultPrepare);

        List<String> questionTexts =
                result.getRecommendedQuestions();

        List<ConsultRecommendedQuestion> questions = new ArrayList<>();

        for (int i = 0; i < questionTexts.size(); i++) {
            questions.add(
                    new ConsultRecommendedQuestion(
                            consultCardId,
                            i + 1,
                            questionTexts.get(i)
                    )
            );
        }

        consultRecommendedQuestionRepository.saveAll(questions);

        log.info(
                "추천 질문 저장 완료. consultCardId={}, count={}",
                consultCardId,
                questions.size()
        );

        return questionTexts;
    }

    private List<String> toQuestionTexts(
            List<ConsultRecommendedQuestion> entities
    ) {

        return entities.stream()
                .map(ConsultRecommendedQuestion::getQuestion)
                .toList();
    }

}
