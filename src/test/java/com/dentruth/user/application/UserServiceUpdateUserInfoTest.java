package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.UpdateUserInfoApplicationRequest;
import com.dentruth.user.application.dto.response.UserInfoResponse;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateUserInfoTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @DisplayName("유저 정보 업데이트에 성공한다.")
    @Test
    void shouldSucceedUpdateUserInfo_whenRequestIsValid() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        UpdateUserInfoApplicationRequest request = getUpdateUserInfoApplicationRequest();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(any(),anyString())).willReturn("token");

        //when
        UserInfoResponse userInfoResponse = userService.updateUserInfo(userId, request);

        //then
        assertThat(userInfoResponse.getName()).isEqualTo(request.getName());
        assertThat(userInfoResponse.getLanguage()).isEqualTo(request.getLanguage().name());
        assertThat(userInfoResponse.getBirth().toString()).isEqualTo(request.getBirthDate().toString());
        assertThat(userInfoResponse.getGender()).isEqualTo(request.getGender().getKo());
        assertThat(userInfoResponse.getRegion()).isEqualTo(request.getRegion());
        assertThat(userInfoResponse.getNationality()).isEqualTo(request.getNationality());
        assertThat(userInfoResponse.getStayDuration()).isEqualTo(request.getStayDuration().getKo());
        assertThat(userInfoResponse.getInsuranceStatus()).isEqualTo(request.getInsuranceStatus().getKo());
        assertThat(userInfoResponse.getAccessToken()).isEqualTo("token");
    }

    @DisplayName("유저가 WITHDRAWN, DELETED 상태면 예외가 발생하고 유저 정보 업데이트에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowException_whenUserIsWithdrawnOrDeletedDuringUpdate(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, status);
        UpdateUserInfoApplicationRequest request = getUpdateUserInfoApplicationRequest();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("유저가 SUSPENDED 상태면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsSuspendedDuringUpdate() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.SUSPENDED);
        UpdateUserInfoApplicationRequest request = getUpdateUserInfoApplicationRequest();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());
    }

    @DisplayName("유저가 BLOCKED 상태면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsBlockedDuringUpdate() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.BLOCKED);
        UpdateUserInfoApplicationRequest request = getUpdateUserInfoApplicationRequest();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());
    }

    @DisplayName("유저가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotExistDuringUpdate() {
        //given
        UUID userId = UUID.randomUUID();
        UpdateUserInfoApplicationRequest request = getUpdateUserInfoApplicationRequest();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("업데이트할 유저 이름이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUpdateNameIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("업데이트할 유저 이름이 2자 미만이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUpdateNameIsLessThanTwoCharacters() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("업데이트할 유저 이름이 50자 초과면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUpdateNameExceedsFiftyCharacters() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이".repeat(51))
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("생년월일이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("생년월일이 미래 날짜면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsInFuture() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.now().plusDays(1))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("생년월일이 150년 이상의 과거라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsMoreThan150YearsAgo() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.now().minusYears(150).minusDays(1))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("거주 지역이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRegionIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("선택된 언어가 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenLanguageIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("성별이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenGenderIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 21))
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("체류 기간이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenStayDurationIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @DisplayName("보험 상태가 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenInsuranceStatusIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.updateUserInfo(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BAD_REQUEST.getMessage());
    }

    private UpdateUserInfoApplicationRequest getUpdateUserInfoApplicationRequest() {
        return UpdateUserInfoApplicationRequest.builder()
                .name("업데이트 할 이름")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();
    }

    private User getUser(UUID userId, UserStatus status) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .status(status)
                .userType(UserType.LOCAL)
                .name("기존 이름")
                .language(Language.KOREAN)
                .birth(LocalDate.of(2002, 5, 21))
                .gender(Gender.F)
                .region("서울 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();
    }

}
