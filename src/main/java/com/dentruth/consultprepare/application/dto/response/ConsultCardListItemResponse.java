package com.dentruth.consultprepare.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ConsultCardListItemResponse {

    private Long consultCardId;

    private String consultTitle;

    private LocalDate consultDate;

    private Boolean consultStatus;
}
