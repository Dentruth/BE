package com.dentruth.consultprepare.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConsultTranslationResult {

    private String painOrigin;
    private String painKo;
    private String painLocationKo;
    private String visitPurpose;
}
