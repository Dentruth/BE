package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.SignupApplicationRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthV1ServiceSignupTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @DisplayName("이미 가입된 유저가 아니라면 로컬 회원가입에 성공한다.")
    @Test
    void shouldSucceedLocalSignUp_withStatusLocalAndUserTypeActive_whenUserIsNotRegistered() {
        //given
        String encodedPassword = "ENCODED_PASSWORD";

        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.getPassword())).willReturn(encodedPassword);

        //when, then
        assertDoesNotThrow(() -> authService.signup(request));

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any());
    }

    @DisplayName("이미 가입된 계정이 게스트, 정지, 차단, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldThrowException_whenSignUpWithAlreadyRegisteredEmail(String status, UserStatus userStatus)
            throws Exception {
        //given
        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);

        given(user.getStatus()).willReturn(userStatus);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.ALREADY_REGISTERED_EMAIL.getMessage());

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, never()).encode(request.getPassword());
        verify(userRepository, never()).save(any());
    }

    private static Stream<Arguments> provideUserStatuses() {
        return Stream.of(
                Arguments.of("게스트", UserStatus.GUEST),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("정상", UserStatus.ACTIVE)
        );
    }

    @DisplayName("이미 가입된 계정이 탈퇴한 계정이거나 삭제된 계정이라면 회원가입에 성공한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideWithdrawableOrDeletedStatuses")
    void shouldSucceedSignUp_whenAccountIsWithdrawnOrDeleted(String status, UserStatus userStatus) throws Exception {
        //given
        String encodedPassword = "ENCODED_PASSWORD";

        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(user.getStatus()).willReturn(userStatus);
        given(passwordEncoder.encode(request.getPassword())).willReturn(encodedPassword);

        //when, then
        assertDoesNotThrow(() -> authService.signup(request));

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any());
    }

    private static Stream<Arguments> provideWithdrawableOrDeletedStatuses(){
        return Stream.of(
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("탈퇴", UserStatus.WITHDRAWN)
        );
    }

    private SignupApplicationRequest createSignupApplicationRequest() {
        return SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .language(Language.KOREAN)
                .birthDate(LocalDate.of(2026, 5, 19))
                .gender(Gender.F)
                .region("서울시 강남구")
                .nationality("미국")
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .build();
    }

}
