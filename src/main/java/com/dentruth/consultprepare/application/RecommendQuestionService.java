package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.response.RecommendQuestionResult;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;

public interface RecommendQuestionService {

    RecommendQuestionResult recommendQuestions(
            ConsultPrepare consultPrepare
    );

}
