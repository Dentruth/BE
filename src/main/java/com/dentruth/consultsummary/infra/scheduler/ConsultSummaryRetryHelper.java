package com.dentruth.consultsummary.infra.scheduler;

import com.dentruth.consultsummary.domain.entity.ConsultSummary;
import com.dentruth.consultsummary.domain.entity.enums.SummaryStatus;
import com.dentruth.consultsummary.domain.repository.ConsultSummaryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ConsultSummaryRetryHelper {

    private final ConsultSummaryRepository consultSummaryRepository;

    @Transactional
    public List<ConsultSummary> getFailedConsultSummaries() {
        List<ConsultSummary> summaries = consultSummaryRepository.findAllByStatusAndIsDeletedFalse(SummaryStatus.FAILED);

        summaries.forEach(s -> s.changeStatus(SummaryStatus.RETRYING));

        return summaries;
    }

}
