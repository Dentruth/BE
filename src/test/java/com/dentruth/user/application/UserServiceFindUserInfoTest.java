package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
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
class UserServiceFindUserInfoTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("유저 정보가 존재하고, GUEST, ACTIVE 상태라면 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldSucceedRetrieval_whenUserExistsAndStatusIsActiveOrGuest(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, Language.KOREAN, userStatus);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when
        UserInfoResponse userInfo = userService.getUserInfo(userId);

        //then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getName()).isEqualTo(user.getName());
        assertThat(userInfo.getLanguage()).isEqualTo(user.getLanguage().name());
        assertThat(userInfo.getBirth().toString()).isEqualTo(user.getBirth().toString());
        assertThat(userInfo.getGender()).isEqualTo(user.getGender().getKo());
        assertThat(userInfo.getRegion()).isEqualTo(user.getRegion());
        assertThat(userInfo.getStayDuration()).isEqualTo(user.getStayDuration().getKo());
        assertThat(userInfo.getInsuranceStatus()).isEqualTo(user.getInsuranceStatus().getKo());
        assertThat(userInfo.getNationality()).isEqualTo(user.getNationality());
    }

    @DisplayName("Language가 ENGLISH거나 SPANISH면 영어로 결과가 반환된다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = Language.class, names = {"ENGLISH", "SPANISH"})
    void shouldReturnInfoInEnglish_whenLanguageIsEnglishOrSpanish(Language language) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, language, UserStatus.ACTIVE);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when
        UserInfoResponse userInfo = userService.getUserInfo(userId);

        //then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getName()).isEqualTo(user.getName());
        assertThat(userInfo.getLanguage()).isEqualTo(user.getLanguage().name());
        assertThat(userInfo.getBirth().toString()).isEqualTo(user.getBirth().toString());
        assertThat(userInfo.getGender()).isEqualTo(user.getGender().getEng());
        assertThat(userInfo.getRegion()).isEqualTo(user.getRegion());
        assertThat(userInfo.getStayDuration()).isEqualTo(user.getStayDuration().getEng());
        assertThat(userInfo.getInsuranceStatus()).isEqualTo(user.getInsuranceStatus().getEng());
        assertThat(userInfo.getNationality()).isEqualTo(user.getNationality());
    }

    @DisplayName("Language가 KOREAN이면 한국어로 결과가 반환된다.")
    @Test
    void shouldReturnInfoInKorean_whenLanguageIsKorean() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, Language.KOREAN, UserStatus.ACTIVE);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when
        UserInfoResponse userInfo = userService.getUserInfo(userId);

        //then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getName()).isEqualTo(user.getName());
        assertThat(userInfo.getLanguage()).isEqualTo(user.getLanguage().name());
        assertThat(userInfo.getBirth().toString()).isEqualTo(user.getBirth().toString());
        assertThat(userInfo.getGender()).isEqualTo(user.getGender().getKo());
        assertThat(userInfo.getRegion()).isEqualTo(user.getRegion());
        assertThat(userInfo.getStayDuration()).isEqualTo(user.getStayDuration().getKo());
        assertThat(userInfo.getInsuranceStatus()).isEqualTo(user.getInsuranceStatus().getKo());
        assertThat(userInfo.getNationality()).isEqualTo(user.getNationality());
    }

    @DisplayName("유저 정보가 존재하지만 WITHDRAWN이거나 DELETED이면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowException_whenUserExistsButStatusIsWithdrawnOrDeleted(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, Language.KOREAN, userStatus);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("유저 정보가 존재하지만 SUSPENDED이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserExistsButStatusIsSuspended() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, Language.KOREAN, UserStatus.SUSPENDED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());
    }

    @DisplayName("유저 정보가 존재하지만 BLOCKED이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserExistsButStatusIsBlocked() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, Language.KOREAN, UserStatus.BLOCKED);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());
    }

    @DisplayName("유저 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

    private User getUser(UUID userId, Language language, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .region("Region")
                .nationality("nationality")
                .email("test@test.com")
                .password("password12345!")
                .userType(UserType.LOCAL)
                .name("name")
                .birth(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .language(language)
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .build();
    }

}
