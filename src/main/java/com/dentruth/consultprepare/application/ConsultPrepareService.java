package com.dentruth.consultprepare.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultprepare.application.dto.request.CreateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.request.UpdateConsultCardRequest;
import com.dentruth.consultprepare.application.dto.response.*;
import com.dentruth.consultprepare.domain.entity.*;
import com.dentruth.consultprepare.domain.entity.enums.DrinkingLevel;
import com.dentruth.consultprepare.domain.entity.enums.PainLevel;
import com.dentruth.consultprepare.domain.repository.*;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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
    private final ConsultTranslationService consultTranslationService;
    private final ConsultRecommendedQuestionService consultRecommendedQuestionService;


    public CreateConsultCardResponse createConsultCard(
            UUID userId,
            CreateConsultCardRequest request
    ) {

        log.info("상담카드 생성 요청 userId={}", userId);

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

        saveDentalHistories(
                saved.getId(),
                request.getMedicalHistories()
                        .getDentalHistories()
        );

        saveMedicalHistories(
                saved.getId(),
                request.getMedicalHistories()
                        .getMedicalHistories()
        );

        log.info(
                "[상담카드 생성] 저장 완료 consultPrepareId={}, userId={}",
                saved.getId(),
                userId
        );

        return new CreateConsultCardResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<ConsultCardListItemResponse> getConsultCards(
            UUID userId
    ) {

        LocalDate today = LocalDate.now();

        return consultPrepareRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByAppointmentDateDesc(userId)
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

    private void saveDentalHistories(
            Long consultPrepareId,
            List<String> dentalHistoryNames
    ) {

        if (dentalHistoryNames == null ||
                dentalHistoryNames.isEmpty()) {
            return;
        }

        for (String name : dentalHistoryNames) {

            DentalHistory dentalHistory =
                    dentalHistoryRepository.findByName(name)
                            .orElseThrow(() -> {
                                log.info(
                                        "존재하지 않는 치과 병력입니다. name={}",
                                        name
                                );
                                return new DentruthException(
                                        ErrorStatus.INVALID_DENTAL_HISTORY
                                );
                            });

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
                            .orElseThrow(() -> {
                                log.info(
                                        "존재하지 않는 전신 병력입니다. name={}",
                                        name
                                );
                                return new DentruthException(
                                        ErrorStatus.INVALID_MEDICAL_HISTORY
                                );
                            });

            consultMedicalHistoryRepository.save(
                    new ConsultMedicalHistory(
                            consultPrepareId,
                            medicalHistory.getId()
                    )
            );
        }
    }

    @Transactional
    public ConsultCardDetailResponse getConsultCardDetail(
            UUID userId,
            Long consultCardId
    ) {

        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                consultCardId,
                                userId
                        ).orElseThrow(() -> {
                            log.info(
                                    "상담카드가 존재하지 않습니다. consultCardId={}",
                                    consultCardId
                            );
                            return new DentruthException(
                                    ErrorStatus.CONSULT_CARD_NOT_FOUND
                            );
                        });

        User user =
                userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.info(
                                "유저 정보가 존재하지 않습니다. userId={}",
                                userId
                        );
                        return new DentruthException(
                                ErrorStatus.USER_NOT_FOUND
                        );
                    });

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

        List<String> recommendedQuestions =
                consultRecommendedQuestionService
                        .generateIfAbsent(consultPrepare);

        String painLevelDuration =
                getPainLevel(consultPrepare.getPainLevel())
                        + " · "
                        + consultPrepare.getPainDuration();

        String socialHistory =
                getSocialHistory(
                        consultPrepare.getDrinking()
                );

        String concerns =
                consultPrepare.getWorriedIssue()
                        + " / "
                        + consultPrepare.getQuestion();

        return new ConsultCardDetailResponse(
                consultPrepare.getAppointmentDate()
                        .toLocalDate(),
                stayStatus,
                consultPrepare.getPainLocation(),
                painLevelDuration,
                socialHistory,
                dentalHistories,
                medicalHistories,
                concerns,
                recommendedQuestions
        );
    }

    private String getSocialHistory(
            DrinkingLevel drinkingLevel
    ) {

        return switch (drinkingLevel) {
            case NON_DRINK -> "Non Drinker";
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

    @Transactional
    public void updateConsultCard(
            UUID userId, Long consultCardId,
            UpdateConsultCardRequest request
    ) {

        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                consultCardId,
                                userId
                        )
                        .orElseThrow(() -> {
                            log.info(
                                    "상담카드가 존재하지 않습니다. consultCardId={}, userId={}",
                                    consultCardId,
                                    userId
                            );
                            return new DentruthException(
                                    ErrorStatus.CONSULT_CARD_NOT_FOUND
                            );
                        });

        consultPrepare.update(
                request.getTitle(),
                request.getVisitInfo()
                        .getVisitDate()
                        .atStartOfDay(),
                request.getVisitInfo()
                        .getCurrentStatus(),
                request.getSymptomInfo()
                        .getPainExistence(),
                request.getSymptomInfo()
                        .getPainArea(),
                request.getSymptomInfo()
                        .getPainLevel(),
                request.getSymptomInfo()
                        .getPainPersistence(),
                request.getSymptomInfo()
                        .getPainDuration(),
                request.getMemoInfo()
                        .getConcerns(),
                request.getMemoInfo()
                        .getQuestion(),
                request.getMedicalHistories()
                        .getSocialHistory()
                        .getSmoking(),
                request.getMedicalHistories()
                        .getSocialHistory()
                        .getDrinking(),
                request.getMedicalHistories()
                        .getSocialHistory()
                        .getExercise()
        );

        consultDentalHistoryRepository
                .deleteAllByConsultPrepareId(
                        consultCardId
                );

        consultMedicalHistoryRepository
                .deleteAllByConsultPrepareId(
                        consultCardId
                );

        saveDentalHistories(
                consultCardId,
                request.getMedicalHistories()
                        .getDentalHistories()
        );

        saveMedicalHistories(
                consultCardId,
                request.getMedicalHistories()
                        .getMedicalHistories()
        );

        log.info(
                "[상담카드 수정] consultPrepareId={}, userId={}",
                consultCardId,
                userId
        );
    }

    @Transactional
    public void deleteConsultCard(
            UUID userId,
            Long consultCardId
    ) {

        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                consultCardId,
                                userId
                        )
                        .orElseThrow(() -> {
                            log.info(
                                    "상담카드가 존재하지 않습니다. consultCardId={}, userId={}",
                                    consultCardId,
                                    userId
                            );
                            return new DentruthException(
                                    ErrorStatus.CONSULT_CARD_NOT_FOUND
                            );
                        });

        consultPrepare.softDelete();

        log.info(
                "[상담카드 삭제] consultPrepareId={}, userId={}",
                consultCardId,
                userId
        );
    }

    @Transactional(readOnly = true)
    public ConsultDentistResponse getConsultPatient(
            Long consultCardId,
            UUID userId
    ) {
        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                consultCardId,
                                userId
                        ).orElseThrow(() -> {
                            log.info(
                                    "상담카드가 존재하지 않습니다. consultCardId={}",
                                    consultCardId
                            );
                            return new DentruthException(
                                    ErrorStatus.CONSULT_CARD_NOT_FOUND
                            );
                        });

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() -> {
                            log.info(
                                    "유저 정보가 존재하지 않습니다. userId={}",
                                    userId
                            );
                            return new DentruthException(
                                    ErrorStatus.USER_NOT_FOUND
                            );
                        });

        ConsultTranslationResult translation;

        try {

        log.info(
                "OpenAI 상담카드 번역 요청. consultCardId={}, userId={}",
                consultCardId,
                userId
        );


        translation =
                consultTranslationService.translate(
                        createPainOrigin(consultPrepare),
                        consultPrepare.getWorriedIssue(),
                        consultPrepare.getQuestion()
                );

        log.info(
                "OpenAI 상담카드 번역 완료. consultCardId={}, userId={}",
                consultCardId,
                userId
        );

        } catch (Exception e) {

            log.error(
                    "OpenAI 상담카드 번역 실패. consultCardId={}, userId={}",
                    consultCardId,
                    userId,
                    e
            );

            throw e;
        }

        return ConsultDentistResponse.builder()
                .insuranceStatus(user.getInsuranceStatus().getKo())
                .painSummary(
                        ConsultDentistResponse.PainSummary.builder()
                                .painOrigin(translation.getPainOrigin())
                                .painKo(translation.getPainKo())
                                .build()
                )
                .summaryInfo(createSummaryInfo(user, consultPrepare,translation))
                .visitPurpose(translation.getVisitPurpose())
                .build();
    }

    private ConsultDentistResponse.SummaryInfo createSummaryInfo(
            User user,
            ConsultPrepare consultPrepare,
            ConsultTranslationResult translation
    ) {

        return ConsultDentistResponse.SummaryInfo.builder()
                .stayStatus(user.getStayDuration().getKo())
                .painLocation(translation.getPainLocationKo())
                .painInfo(createPainInfo(consultPrepare))
                .dentalHistory(createDentalHistory(consultPrepare))
                .medicalHistory(createMedicalHistory(consultPrepare))
                .socialHistory(createSocialHistory(consultPrepare))
                .build();
    }

    private String createPainOrigin(ConsultPrepare consultPrepare) {

        return String.format(
                "%s pain has %s in %s for about %s.",
                consultPrepare.getPainLevel().getEng().toLowerCase(),
                consultPrepare.getPainPersistence().getEng().toLowerCase(),
                consultPrepare.getPainLocation(),
                consultPrepare.getPainDuration()
        );
    }

    private String createPainInfo(ConsultPrepare consultPrepare) {

        return consultPrepare.getPainLevel().getKo()
                + " / "
                + consultPrepare.getPainDuration();
    }

    private List<String> createMedicalHistory(
            ConsultPrepare consultPrepare
    ) {

        List<String> medicalHistories =
                consultMedicalHistoryRepository.findMedicalHistoryNames(
                        consultPrepare.getId()
                );

        if (medicalHistories.isEmpty()) {
            return List.of("특이사항 없음");
        }

        return medicalHistories;

    }

    private List<String> createDentalHistory(ConsultPrepare consultPrepare) {

        List<String> dentalHistories =
                consultDentalHistoryRepository.findDentalHistoryNames(
                        consultPrepare.getId()
                );

        if (dentalHistories.isEmpty()) {
            return List.of("특이사항 없음");
        }

        return dentalHistories;
    }

    private String createSocialHistory(ConsultPrepare consultPrepare) {

        return String.join(", ",
                consultPrepare.getSmoking().getKo(),
                consultPrepare.getDrinking().getKo(),
                consultPrepare.getExercise().getKo()
        );
    }


    @Transactional
    public RecommendQuestionResponse regenerateRecommendQuestions(
            Long consultCardId,
            UUID userId
    ) {
        ConsultPrepare consultPrepare =
                consultPrepareRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                consultCardId,
                                userId
                        ).orElseThrow(() -> {
                            log.info(
                                    "상담카드가 존재하지 않습니다. consultCardId={}","userId={}",
                                    consultCardId,
                                    userId
                            );
                            return new DentruthException(
                                    ErrorStatus.CONSULT_CARD_NOT_FOUND
                            );
                        });

        List<String> recommendedQuestions =
                consultRecommendedQuestionService
                        .regenerate(consultPrepare);

        return RecommendQuestionResponse.builder()
                .consultCardId(
                        consultCardId
                )
                .recommendedQuestions(
                        recommendedQuestions
                )
                .build();
    }

}
