package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.response.ConsultTranslationResult;

public interface ConsultTranslationService {

    ConsultTranslationResult translate(
            String painOrigin,
            String worriedIssue,
            String question
    );

}
