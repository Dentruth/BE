package com.dentruth.consultprepare.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultprepare.application.dto.response.ConsultCardDetailResponse;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.entity.enums.PainLevel;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultCardFacade {

    private final ConsultPrepareService consultPrepareService;
    private final UserRepository userRepository;
    private final ConsultRecommendedQuestionService consultRecommendedQuestionService;

    public ConsultCardDetailResponse getConsultCardDetail(
            UUID userId,
            Long consultCardId
    ) {

        ConsultPrepare consultPrepare =
                consultPrepareService.findOwnedConsultPrepare(
                        userId,
                        consultCardId
                );

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

        List<String> dentalHistories =
                consultPrepareService
                        .getDentalHistoryNames(consultCardId);

        List<String> medicalHistories =
                consultPrepareService
                        .getMedicalHistoryNames(consultCardId);

        List<String> recommendedQuestions =
                consultRecommendedQuestionService
                        .generateIfAbsent(consultPrepare);

        PainLevel painLevel = consultPrepare.getPainLevel();

        String painLevelDuration =
                (painLevel == null ? "-" : painLevel.getDescription())
                        + " · "
                        + consultPrepare.getPainDuration();

        String socialHistory =
                consultPrepare.getDrinking().getDescription();

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

}
