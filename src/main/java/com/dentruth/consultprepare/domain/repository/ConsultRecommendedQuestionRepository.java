package com.dentruth.consultprepare.domain.repository;

import com.dentruth.consultprepare.domain.entity.ConsultRecommendedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultRecommendedQuestionRepository
        extends JpaRepository<ConsultRecommendedQuestion, Long> {

    List<ConsultRecommendedQuestion>
    findAllByConsultPrepareIdOrderByQuestionOrderAsc(
            Long consultPrepareId
    );

    void deleteAllByConsultPrepareId(
            Long consultPrepareId
    );

}
