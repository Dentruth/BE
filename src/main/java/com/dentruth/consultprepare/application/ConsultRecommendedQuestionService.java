package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.entity.ConsultRecommendedQuestion;
import com.dentruth.consultprepare.domain.repository.ConsultRecommendedQuestionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultRecommendedQuestionService {

    private final RecommendQuestionService recommendQuestionService;
    private final ConsultRecommendedQuestionRepository consultRecommendedQuestionRepository;
    private final ConsultRecommendedQuestionWriter consultRecommendedQuestionWriter;

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

        List<String> questionTexts =
                requestRecommendedQuestions(consultPrepare);

        consultRecommendedQuestionWriter.save(consultCardId, questionTexts);

        log.info(
                "추천 질문 저장 완료. consultCardId={}, count={}",
                consultCardId,
                questionTexts.size()
        );

        return questionTexts;
    }

    public List<String> regenerate(
            ConsultPrepare consultPrepare
    ) {

        Long consultCardId = consultPrepare.getId();

        log.info(
                "추천 질문 재생성 시작. consultCardId={}",
                consultCardId
        );

        List<String> questionTexts =
                requestRecommendedQuestions(consultPrepare);

        consultRecommendedQuestionWriter.replaceAll(consultCardId, questionTexts);

        log.info(
                "추천 질문 저장 완료. consultCardId={}, count={}",
                consultCardId,
                questionTexts.size()
        );

        return questionTexts;
    }

    private List<String> requestRecommendedQuestions(
            ConsultPrepare consultPrepare
    ) {

        return recommendQuestionService
                .recommendQuestions(consultPrepare)
                .getRecommendedQuestions();
    }

    private List<String> toQuestionTexts(
            List<ConsultRecommendedQuestion> entities
    ) {

        return entities.stream()
                .map(ConsultRecommendedQuestion::getQuestion)
                .toList();
    }

}
