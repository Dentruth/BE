package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.UpdatePasswordApplicationRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import java.time.LocalDate;
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
class UserFacadeUpdatePasswordTest {

    @InjectMocks
    private UserFacade userFacade;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태이며 기존 비밀번호가 일치하면 비밀번호 변경에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldChangePasswordSuccessfully_whenUserExistsAndIsActiveOrGuestAndPasswordMatches(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
                .id(userId)
                .region("Region")
                .nationality("nationality")
                .email("test@test.com")
                .password("password1234!")
                .userType(UserType.LOCAL)
                .name("name")
                .birth(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .language(Language.KOREAN)
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .build();

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);
        willDoNothing().given(authService).verifyPassword(request.getExistingPassword(), user.getPassword());

        //when, then
        userFacade.updatePassword(userId, request);

        verify(authService, times(1)).updatePassword(user, request.getNewPassword());
    }

    @DisplayName("유저가 WITHDRAWN 상태거나 DELETED 상태라면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowException_whenUserIsAlreadyWithdrawnOrDeleted(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
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

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updatePassword(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());

        assertThat(user.getStatus()).isEqualTo(status);
        assertThat(user.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 BLOCKED 상태라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsBlocked() {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .status(UserStatus.BLOCKED)
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

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updatePassword(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.BLOCKED_USER.getMessage());

        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 SUSPENDED 상태라면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsSuspended() {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .status(UserStatus.SUSPENDED)
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

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updatePassword(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.SUSPENDED_USER.getMessage());

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getDeletedAt()).isNull();
    }

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태지만 비밀번호가 일치하지 않으면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldThrowException_whenUserIsActiveOrGuestButPasswordDoesNotMatch(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
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

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);
        willThrow(new DentruthException(ErrorStatus.WRONG_PASSWORD))
                .given(authService)
                .verifyPassword(request.getExistingPassword(), user.getPassword());

        //when, then
        assertThatThrownBy(() -> userFacade.updatePassword(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.WRONG_PASSWORD.getMessage());
    }

}
