package com.dentruth.consultprepare.application;

import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.ConsultCardDetailResponse;
import com.dentruth.consultprepare.application.dto.response.ConsultCardListItemResponse;
import com.dentruth.consultprepare.application.dto.response.CreateConsultCardResponse;
import com.dentruth.consultprepare.domain.entity.*;
import com.dentruth.consultprepare.domain.entity.enums.DrinkingLevel;
import com.dentruth.consultprepare.domain.entity.enums.PainLevel;
import com.dentruth.consultprepare.domain.repository.*;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultPrepareService {

    private final ConsultPrepareRepository consultPrepareRepository;
    private final UserRepository userRepository;
    private final ConsultDentalHistoryRepository consultDentalHistoryRepository;
    private final DentalHistoryRepository dentalHistoryRepository;
    private final ConsultMedicalHistoryRepository consultMedicalHistoryRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;

    public CreateConsultCardResponse createConsultCard(
            UUID userId,
            CreateConsultCardRequest request
    ) {

        log.info("상담카드 생성 요청 userId={}", userId);

        System.out.println(request.toString());

        ConsultPrepare consultPrepare =
                ConsultPrepare.builder()
                        .userId(userId)
                        .title((request.getTitle()))
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

    @Transactional
    public List<com.dentruth.consultprepare.application.dto.response.ConsultCardListItemResponse> getConsultCards(
            String userId
    ) {

        LocalDate today = LocalDate.now();

        UUID uuid = UUID.fromString(userId);

        return consultPrepareRepository
                .findAllByUserIdOrderByAppointmentDateDesc(uuid)
                .stream()
                .map(consultPrepare ->
                        new ConsultCardListItemResponse(
                                consultPrepare.getId(),
                                consultPrepare.getTitle(),
                                consultPrepare.getAppointmentDate().toLocalDate(),

                                !consultPrepare.getAppointmentDate()
                                        .toLocalDate()
                                        .isAfter(today)

                        )
                )
                .toList();
    }

    @Transactional
    public ConsultCardDetailResponse getConsultCardDetail(
            String userId,
            Long consultCardId
    ) {

        UUID uuid = UUID.fromString(userId);

        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserId(
                                consultCardId,
                                uuid
                        )
                        .orElseThrow(() ->
                                new RuntimeException("상담카드를 찾을 수 없습니다.")
                        );

        User user =
                userRepository.findById(
                        UUID.fromString(userId)
                ).orElseThrow(() ->
                        new RuntimeException("사용자를 찾을 수 없습니다.")
                );

        String stayStatus =
                user.getStayDuration().getEng()
                        + " · "
                        + user.getInsuranceStatus().getEng();

        List<Long> dentalHistoryIds =
                consultDentalHistoryRepository
                        .findAllByConsultPrepareId(
                                consultCardId
                        )
                        .stream()
                        .map(
                                ConsultDentalHistory::getDentalHistoryId
                        )
                        .toList();

        List<String> dentalHistories =
                dentalHistoryRepository
                        .findAllById(dentalHistoryIds)
                        .stream()
                        .map(DentalHistory::getName)
                        .toList();

        List<Long> medicalHistoryIds =
                consultMedicalHistoryRepository
                        .findAllByConsultPrepareId(
                                consultCardId
                        )
                        .stream()
                        .map(
                                ConsultMedicalHistory::getMedicalHistoryId
                        )
                        .toList();

        List<String> medicalHistories =
                medicalHistoryRepository
                        .findAllById(medicalHistoryIds)
                        .stream()
                        .map(MedicalHistory::getName)
                        .toList();

        String painLevel =
                getPainLevel(
                        consultPrepare.getPainLevel()
                );

        String painDuration =
                getPainLevel(
                        consultPrepare.getPainLevel()
                )
                        + " · "
                        + consultPrepare.getPainDuration();

        String socialHistory =
                getSocialHistory(
                        consultPrepare.getDrinking()
                );

        return new ConsultCardDetailResponse(
                consultPrepare.getAppointmentDate()
                        .toLocalDate(),
                stayStatus,
                consultPrepare.getPainLocation(),
                painLevel,
                consultPrepare.getPainDuration(),
                socialHistory,
                dentalHistories,
                medicalHistories,
                consultPrepare.getWorriedIssue()
        );
    }

    private String getSocialHistory(
            DrinkingLevel drinkingLevel
    ) {

        return switch (drinkingLevel) {
            case NON_SMOKER -> "Non Drinker";
            case OCCASIONAL -> "Alcohol Once a Week";
            case REGULAR -> "Alcohol Several Times a Week";
            case HEAVY -> "Alcohol Daily";
        };
    }

    private String getPainLevel(
            PainLevel painLevel
    ) {

        return switch (painLevel) {
            case NONE -> "None";
            case MILD -> "Mild";
            case MODERATE -> "Moderate";
            case SEVERE -> "Severe";
        };
    }


}
