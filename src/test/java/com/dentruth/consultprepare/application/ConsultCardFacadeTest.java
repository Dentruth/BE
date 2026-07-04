package com.dentruth.consultprepare.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.consultprepare.application.dto.response.ConsultCardDetailResponse;
import com.dentruth.consultprepare.domain.entity.ConsultPrepare;
import com.dentruth.consultprepare.domain.entity.enums.CurrentStatus;
import com.dentruth.consultprepare.domain.entity.enums.DrinkingLevel;
import com.dentruth.consultprepare.domain.entity.enums.ExerciseLevel;
import com.dentruth.consultprepare.domain.entity.enums.PainLevel;
import com.dentruth.consultprepare.domain.entity.enums.PainPersistence;
import com.dentruth.consultprepare.domain.entity.enums.SmokingLevel;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultCardFacadeTest {

    @InjectMocks
    private ConsultCardFacade consultCardFacade;

    @Mock
    private ConsultPrepareService consultPrepareService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConsultRecommendedQuestionService consultRecommendedQuestionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long CONSULT_CARD_ID = 1L;

    @DisplayName("상담카드 상세 조회 시 조회한 값들을 조합해 응답을 반환한다.")
    @Test
    void shouldReturnConsultCardDetail_whenConsultCardAndUserExist() {
        //given
        ConsultPrepare consultPrepare = ConsultPrepare.builder()
                .id(CONSULT_CARD_ID)
                .userId(USER_ID)
                .title("정기 검진")
                .appointmentDate(LocalDateTime.of(2026, 7, 10, 0, 0))
                .currentStatus(CurrentStatus.SHORT_STAY)
                .painExistence(true)
                .painLocation("어금니")
                .painLevel(PainLevel.MODERATE)
                .painPersistence(PainPersistence.OCCASIONAL)
                .painDuration("3일")
                .worriedIssue("충치가 걱정돼요")
                .question("발치가 필요한가요?")
                .smoking(SmokingLevel.NON_SMOKER)
                .drinking(DrinkingLevel.OCCASIONAL)
                .exercise(ExerciseLevel.REGULAR)
                .build();

        User user = User.builder()
                .stayDuration(StayDuration.THREE_TO_SIX_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(consultPrepareService.findOwnedConsultPrepare(USER_ID, CONSULT_CARD_ID))
                .willReturn(consultPrepare);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(consultPrepareService.getDentalHistoryNames(CONSULT_CARD_ID))
                .willReturn(List.of("충치"));
        given(consultPrepareService.getMedicalHistoryNames(CONSULT_CARD_ID))
                .willReturn(List.of("고혈압"));
        given(consultRecommendedQuestionService.generateIfAbsent(consultPrepare))
                .willReturn(List.of("어떤 치료를 받아야 하나요?"));

        //when
        ConsultCardDetailResponse response =
                consultCardFacade.getConsultCardDetail(USER_ID, CONSULT_CARD_ID);

        //then
        assertThat(response.getConsultDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(response.getStayStatus()).isEqualTo("3–6 Months · Insured");
        assertThat(response.getPainArea()).isEqualTo("어금니");
        assertThat(response.getPainLevelDuration()).isEqualTo("Moderate · 3일");
        assertThat(response.getSocialHistory()).isEqualTo("Alcohol Once a Week");
        assertThat(response.getDentalHistories()).containsExactly("충치");
        assertThat(response.getMedicalHistories()).containsExactly("고혈압");
        assertThat(response.getConcerns()).isEqualTo("충치가 걱정돼요 / 발치가 필요한가요?");
        assertThat(response.getRecommendedQuestions()).containsExactly("어떤 치료를 받아야 하나요?");
    }

    @DisplayName("painLevel이 없으면 예외 없이 기본값으로 응답을 반환한다.")
    @Test
    void shouldReturnDefaultPainLevel_whenPainLevelIsNull() {
        //given
        ConsultPrepare consultPrepare = ConsultPrepare.builder()
                .id(CONSULT_CARD_ID)
                .userId(USER_ID)
                .title("정기 검진")
                .appointmentDate(LocalDateTime.of(2026, 7, 10, 0, 0))
                .currentStatus(CurrentStatus.SHORT_STAY)
                .painExistence(false)
                .painLocation(null)
                .painLevel(null)
                .painDuration(null)
                .worriedIssue("특이사항 없어요")
                .question("정기 검진 주기가 궁금해요")
                .smoking(SmokingLevel.NON_SMOKER)
                .drinking(DrinkingLevel.NON_DRINK)
                .exercise(ExerciseLevel.REGULAR)
                .build();

        User user = User.builder()
                .stayDuration(StayDuration.THREE_TO_SIX_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(consultPrepareService.findOwnedConsultPrepare(USER_ID, CONSULT_CARD_ID))
                .willReturn(consultPrepare);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(consultPrepareService.getDentalHistoryNames(CONSULT_CARD_ID))
                .willReturn(List.of());
        given(consultPrepareService.getMedicalHistoryNames(CONSULT_CARD_ID))
                .willReturn(List.of());
        given(consultRecommendedQuestionService.generateIfAbsent(consultPrepare))
                .willReturn(List.of());

        //when
        ConsultCardDetailResponse response =
                consultCardFacade.getConsultCardDetail(USER_ID, CONSULT_CARD_ID);

        //then
        assertThat(response.getPainLevelDuration()).isEqualTo("- · null");
        assertThat(response.getSocialHistory()).isEqualTo("Non Drinker");
    }

    @DisplayName("상담카드가 존재하지 않으면 예외가 전파된다.")
    @Test
    void shouldThrowException_whenConsultCardNotFound() {
        //given
        given(consultPrepareService.findOwnedConsultPrepare(USER_ID, CONSULT_CARD_ID))
                .willThrow(new DentruthException(ErrorStatus.CONSULT_CARD_NOT_FOUND));

        //when & then
        assertThatThrownBy(() ->
                consultCardFacade.getConsultCardDetail(USER_ID, CONSULT_CARD_ID))
                .isInstanceOf(DentruthException.class);
    }

    @DisplayName("유저 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserNotFound() {
        //given
        ConsultPrepare consultPrepare = ConsultPrepare.builder()
                .id(CONSULT_CARD_ID)
                .userId(USER_ID)
                .build();

        given(consultPrepareService.findOwnedConsultPrepare(USER_ID, CONSULT_CARD_ID))
                .willReturn(consultPrepare);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() ->
                consultCardFacade.getConsultCardDetail(USER_ID, CONSULT_CARD_ID))
                .isInstanceOf(DentruthException.class);
    }

}
