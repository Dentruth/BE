package com.dentruth.consultsummary.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsultSummariesResponse {

    private final UUID id;
    private final String title;
    private final String clinicName;
    private final LocalDate date;
    private final String diagnosis;

}
