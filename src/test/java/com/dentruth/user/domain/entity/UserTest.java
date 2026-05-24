package com.dentruth.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTest {

    @DisplayName("유저 상태가 ACTIVE라면 예외가 발생하지 않는다.")
    @Test
    void shouldNotThrowException_whenUserStatusIsActive() {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .status(UserStatus.ACTIVE)
                .build();

        //when, then
        assertDoesNotThrow(user::validateStatus);
    }

    @DisplayName("유저 상태가 SUSPENDED라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserStatusIsSuspended() {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .status(UserStatus.SUSPENDED)
                .build();

        //when, then
        assertThatThrownBy(user::validateStatus)
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());
    }

    @DisplayName("유저 상태가 BLOCKED라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserStatusIsBlocked() {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .status(UserStatus.BLOCKED)
                .build();

        //when, then
        assertThatThrownBy(user::validateStatus)
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());
    }

    @DisplayName("유저 상태가 WITHDRAWN라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserStatusIsWithdrawn() {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .status(UserStatus.WITHDRAWN)
                .build();

        //when, then
        assertThatThrownBy(user::validateStatus)
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("유저 상태가 DELETED라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserStatusIsDeleted() {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .status(UserStatus.DELETED)
                .build();

        //when, then
        assertThatThrownBy(user::validateStatus)
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("유저 상태별 이메일 중복 검증 테스트")
    @Nested
    class EmailDuplicationValidationTest {

        @DisplayName("유저 상태가 ACTIVE, SUSPENDED, BLOCKED, GUEST라면 이메일 중복 예외가 발생한다.")
        @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
        @EnumSource(value = UserStatus.class, names = {"ACTIVE", "SUSPENDED", "BLOCKED", "GUEST"})
        void shouldThrowException_whenEmailIsAlreadyRegisteredByStatus(UserStatus status) {
            //given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .status(status)
                    .build();

            //when, then
            assertThatThrownBy(user::validateDuplicationEmailByStatus)
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.ALREADY_REGISTERED_EMAIL.getMessage());
        }

        @DisplayName("유저 상태가 WITHDRAWN, DELETED라면 이메일 중복 예외가 발생하지 않는다.")
        @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
        @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
        void shouldNotThrowException_whenUserIsWithdrawnOrDeleted(UserStatus status) {
            //given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .status(status)
                    .build();

            //when, then
            assertDoesNotThrow(user::validateDuplicationEmailByStatus);
        }
    }

    @DisplayName("유저 정보 업데이트 비즈니스 제약 조건 테스트")
    @Nested
    class UpdateInfoValidationTest {
        private User createBaseUser() {
            return User.builder()
                    .id(UUID.randomUUID())
                    .name("name")
                    .language(Language.KOREAN)
                    .birth(LocalDate.of(2002, 5, 24))
                    .gender(Gender.F)
                    .region("Region")
                    .stayDuration(StayDuration.ONE_TO_THREE_M)
                    .insuranceStatus(InsuranceStatus.INSURED)
                    .nationality("nationality")
                    .status(UserStatus.ACTIVE)
                    .build();
        }

        @DisplayName("올바른 데이터가 주어지면 유저 정보 업데이트에 성공한다.")
        @Test
        void shouldSucceedUpdate_whenDataIsValid() {
            //given
            User user = createBaseUser();
            String expectedName = "새이름";
            Language expectedLanguage = Language.ENGLISH;
            LocalDate expectedBirth = LocalDate.of(2000, 1, 1);
            Gender expectedGender = Gender.M;
            String expectedRegion = "NewRegion";
            StayDuration expectedStay = StayDuration.ONE_TO_THREE_M;
            InsuranceStatus expectedInsurance = InsuranceStatus.NOT_INSURED;
            String expectedNationality = "NewNationality";

            //when, then
            assertDoesNotThrow(() -> user.updateInfo(expectedName, expectedLanguage, expectedBirth, expectedGender,
                    expectedRegion, expectedStay, expectedInsurance, expectedNationality));

            assertThat(user.getName()).isEqualTo(expectedName);
            assertThat(user.getLanguage()).isEqualTo(expectedLanguage);
            assertThat(user.getBirth()).isEqualTo(expectedBirth);
            assertThat(user.getGender()).isEqualTo(expectedGender);
            assertThat(user.getRegion()).isEqualTo(expectedRegion);
            assertThat(user.getStayDuration()).isEqualTo(expectedStay);
            assertThat(user.getInsuranceStatus()).isEqualTo(expectedInsurance);
            assertThat(user.getNationality()).isEqualTo(expectedNationality);
        }

        @DisplayName("이름이 null이거나 공백, 혹은 2자 미만 20자 초과 시 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest(name = "[{index}] 잘못된 이름: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "A", "이름이이십글자를넘어가는비정상적인케이스입니다글자수를확인하세요"})
        void shouldThrowException_whenNameIsInvalid(String invalidName) {
            //given
            User user = createBaseUser();

            //when, then
            assertThatThrownBy(() -> user.updateInfo(invalidName, Language.KOREAN, LocalDate.of(2002, 5, 24), Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
        }

        @DisplayName("생년월일이 null이거나, 미래 날짜이거나, 150년보다 더 과거 날짜이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void shouldThrowException_whenBirthDateIsInvalid() {
            //given
            User user = createBaseUser();
            LocalDate futureDate = LocalDate.now().plusDays(1);
            LocalDate tooPastDate = LocalDate.now().minusYears(151);

            //when, then
            //Null 체크
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, null, Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            //미래 날짜 체크
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, futureDate, Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            //비정상 과거 날짜 체크
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, tooPastDate, Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
        }

        @DisplayName("거주지역이나 국적이 비어있으면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest(name = "[{index}] 빈 입력값: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void shouldThrowException_whenLocationOrIdentityIsInvalid(String invalidInput) {
            //given
            User user = createBaseUser();

            //when, then
            // 거주지역 공백 예외 검증
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, LocalDate.of(2002, 5, 24), Gender.F,
                    invalidInput, StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            // 국적 공백 예외 검증
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, LocalDate.of(2002, 5, 24), Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, invalidInput))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
        }

        @DisplayName("필수 선택 Enum값(Language, Gender, StayDuration, InsuranceStatus)이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void shouldThrowException_whenRequiredEnumIsNull() {
            //given
            User user = createBaseUser();

            // Language가 null일 때
            assertThatThrownBy(() -> user.updateInfo("name", null, LocalDate.of(2002, 5, 24), Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            // Gender가 null일 때
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, LocalDate.of(2002, 5, 24), null,
                    "Region", StayDuration.ONE_TO_THREE_M, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            // StayDuration이 null일 때
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, LocalDate.of(2002, 5, 24), Gender.F,
                    "Region", null, InsuranceStatus.INSURED, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());

            // InsuranceStatus가 null일 때
            assertThatThrownBy(() -> user.updateInfo("name", Language.KOREAN, LocalDate.of(2002, 5, 24), Gender.F,
                    "Region", StayDuration.ONE_TO_THREE_M, null, "nationality"))
                    .isInstanceOf(DentruthException.class)
                    .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
        }
    }

}
