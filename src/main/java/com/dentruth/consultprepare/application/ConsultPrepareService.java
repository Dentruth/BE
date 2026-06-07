package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.request.ConsultCardListItemResponse;
import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.CreateConsultCardResponse;
import com.dentruth.consultprepare.domain.entity.*;
import com.dentruth.consultprepare.domain.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultPrepareService {

    private final ConsultPrepareRepository consultPrepareRepository;
    private final DentalHistoryRepository dentalHistoryRepository;
    private final ConsultDentalHistoryRepository consultDentalHistoryRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final ConsultMedicalHistoryRepository consultMedicalHistoryRepository;

    public CreateConsultCardResponse createConsultCard(
            String userId,
            CreateConsultCardRequest request
    ) {

        log.info("상담카드 생성 요청 userId={}", userId);

        System.out.println(request.toString());

        ConsultPrepare consultPrepare =
                ConsultPrepare.builder()
                        .title(
                                request.getTitle()
                        )
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

        saveDentalHistories(
                saved.getId(),
                request.getMedicalHistories().getDentalHistories()
        );

        saveMedicalHistories(
                saved.getId(),
                request.getMedicalHistories().getMedicalHistories()
        );

        log.info(
                "[상담카드 생성] 저장 완료 consultPrepareId={}, userId={}",
                saved.getId(),
                userId
        );

        return new CreateConsultCardResponse(saved.getId());
    }

    private void saveDentalHistories(
            Long consultPrepareId,
            List<String> dentalHistoryNames
    ) {

        for (String name : dentalHistoryNames) {

            DentalHistory dentalHistory =
                    dentalHistoryRepository.findByName(name)
                            .orElseThrow(() ->
                                    new RuntimeException("존재하지 않는 치과 병력")
                            );

            consultDentalHistoryRepository.save(
                    new ConsultDentalHistory(
                            consultPrepareId,
                            dentalHistory.getId()
                    )
            );
        }
    }

    private void saveMedicalHistories(
            Long consultPrepareId,
            List<String> medicalHistoryNames
    ) {

        if (medicalHistoryNames == null ||
                medicalHistoryNames.isEmpty()) {
            return;
        }

        for (String name : medicalHistoryNames) {

            MedicalHistory medicalHistory =
                    medicalHistoryRepository.findByName(name)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "존재하지 않는 병력"
                                    )
                            );

            consultMedicalHistoryRepository.save(
                    new ConsultMedicalHistory(
                            consultPrepareId,
                            medicalHistory.getId()
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ConsultCardListItemResponse> getConsultCards(
            String userId
    ) {

        List<ConsultPrepare> consultCards =
                consultPrepareRepository
                        .findAllByUserIdOrderByAppointmentDateDesc(userId);

        return consultCards.stream()
                .map(card -> new ConsultCardListItemResponse(
                        card.getId(),
                        card.getTitle(),
                        card.getAppointmentDate().toLocalDate(),
                        !card.getAppointmentDate()
                                .toLocalDate()
                                .isBefore(LocalDate.now())
                ))
                .toList();
    }
}
