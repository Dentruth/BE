package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.CreateConsultCardResponse;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.repository.ConsultPrepareRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultPrepareService {

    private final ConsultPrepareRepository consultPrepareRepository;

    public CreateConsultCardResponse createConsultCard(
            String userId,
            CreateConsultCardRequest request
    ) {

        log.info("상담카드 생성 요청 userId={}", userId);

        System.out.println(request.toString());

        ConsultPrepare consultPrepare =
                ConsultPrepare.builder()
                        .userId(userId)
                        .appointmentDate(
                                request.getVisitInfo()
                                        .getVisitDate()
                                        .atStartOfDay()
                        )
                        .currentStatus(
                                request.getVisitInfo()
                                        .getCurrentStatus()
                        )
                        .painExistence(
                                request.getSymptomInfo()
                                        .getPainExistence()
                        )
                        .painLocation(
                                request.getSymptomInfo()
                                        .getPainArea()
                        )
                        .painLevel(
                                request.getSymptomInfo()
                                        .getPainLevel()
                        )
                        .painPersistence(
                                request.getSymptomInfo()
                                        .getPainPersistence()
                        )
                        .painDuration(
                                request.getSymptomInfo()
                                        .getPainDuration()
                        )
                        .worriedIssue(
                                request.getMemoInfo()
                                        .getConcerns()
                        )
                        .question(
                                request.getMemoInfo()
                                        .getQuestion()
                        )
                        .smoking(
                                request.getMedicalHistories()
                                        .getSocialHistory()
                                        .getSmoking()
                        )
                        .drinking(
                                request.getMedicalHistories()
                                        .getSocialHistory()
                                        .getDrinking()
                        )
                        .exercise(
                                request.getMedicalHistories()
                                        .getSocialHistory()
                                        .getExercise()
                        )
                        .build();

        ConsultPrepare saved =
                consultPrepareRepository.save(consultPrepare);

        log.info(
                "[상담카드 생성] 저장 완료 consultPrepareId={}, userId={}",
                saved.getId(),
                userId
        );

        return new CreateConsultCardResponse(saved.getId());
    }
}
