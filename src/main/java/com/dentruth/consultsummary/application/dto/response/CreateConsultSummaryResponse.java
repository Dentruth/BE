package com.dentruth.consultsummary.application.dto.response;

import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreateConsultSummaryResponse {

    public final UUID id;
    private final String clinicName;
    private final SummaryStatus status;
    private LocalDateTime createdAt;

}
