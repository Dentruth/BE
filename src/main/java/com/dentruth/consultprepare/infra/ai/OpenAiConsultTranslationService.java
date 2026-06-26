package com.dentruth.consultprepare.infra.ai;

public class ConsultTranslationService {

    public interface ConsultTranslationService {

        PainSummaryResult translatePainSummary(
                String painOrigin
        );

        String createVisitPurpose(
                String worriedIssue,
                String question
        );

    }

}
