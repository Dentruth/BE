package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.WithdrawnApplicationRequest;
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
class UserFacadeWithdrawnTest {

    @InjectMocks
    private UserFacade userFacade;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태이고 비밀번호가 일치하면 탈퇴에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldWithdrawSuccessfully_whenUserIsActiveOrGuestAndPasswordMatches(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();

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

        WithdrawnApplicationRequest request = new WithdrawnApplicationRequest("password1234!");

        given(userService.findById(userId, "탈퇴")).willReturn(user);
        willDoNothing().given(authService).verifyPassword(request.getPassword(), user.getPassword());

        //when
        userFacade.deleteUser(userId, request);

        //then
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @DisplayName("유저가 WITHDRAWN 상태거나 DELETED 상태라면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowException_whenUserIsAlreadyWithdrawnOrDeleted(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();

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

        WithdrawnApplicationRequest request = new WithdrawnApplicationRequest("password1234!");

        given(userService.findById(userId, "탈퇴")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.deleteUser(userId, request))
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

        WithdrawnApplicationRequest request = new WithdrawnApplicationRequest("password1234!");

        given(userService.findById(userId, "탈퇴")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.deleteUser(userId, request))
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

        WithdrawnApplicationRequest request = new WithdrawnApplicationRequest("password1234!");

        given(userService.findById(userId, "탈퇴")).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.deleteUser(userId, request))
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

        WithdrawnApplicationRequest request = new WithdrawnApplicationRequest("password1234!");

        given(userService.findById(userId, "탈퇴")).willReturn(user);
        willThrow(new DentruthException(ErrorStatus.WRONG_PASSWORD))
                .given(authService)
                .verifyPassword(request.getPassword(), user.getPassword());

        //when, then
        assertThatThrownBy(() -> userFacade.deleteUser(userId, request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.WRONG_PASSWORD.getMessage());
    }

}
