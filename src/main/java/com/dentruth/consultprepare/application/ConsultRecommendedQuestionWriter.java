package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.domain.entity.ConsultRecommendedQuestion;
import com.dentruth.consultprepare.domain.repository.ConsultRecommendedQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultRecommendedQuestionWriter {

    private final ConsultRecommendedQuestionRepository consultRecommendedQuestionRepository;

    @Transactional
    public void save(
            Long consultCardId,
            List<String> questionTexts
    ) {

        consultRecommendedQuestionRepository.saveAll(
                toEntities(consultCardId, questionTexts)
        );
    }

    @Transactional
    public void replaceAll(
            Long consultCardId,
            List<String> questionTexts
    ) {

        consultRecommendedQuestionRepository
                .deleteAllByConsultPrepareId(consultCardId);

        consultRecommendedQuestionRepository.saveAll(
                toEntities(consultCardId, questionTexts)
        );
    }

    private List<ConsultRecommendedQuestion> toEntities(
            Long consultCardId,
            List<String> questionTexts
    ) {

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

        return questions;
    }

}
