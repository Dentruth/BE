package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceEmailDuplicationTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("이메일 중복 검사시 유저가 존재하지 않으면 예외가 발생하지 않는다.")
    @Test
    void shouldNotThrowException_whenUserDoesNotExistDuringDuplicateCheck() {
        //given
        String email = "test@test.com";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        //when, then
        assertDoesNotThrow(() -> userService.checkEmailDuplication(email));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하고 ACTIVE, GUEST, SUSPENDED, BLOCKED 상태라면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST", "SUSPENDED", "BLOCKED"})
    void shouldThrowException_whenEmailExistsWithActiveOrRestrictedStatus(UserStatus status) {
        //given
        String email = "test@test.com";
        User user = User.builder()
                .email(email)
                .status(status)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.checkEmailDuplication(email))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.ALREADY_REGISTERED_EMAIL.getMessage());
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하지만 DELETED, WITHDRAWN 상태라면 예외가 발생하지 않는다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"DELETED", "WITHDRAWN"})
    void shouldNotThrowException_whenEmailExistsButStatusIsDeletedOrWithdrawn(UserStatus status) throws Exception {
        //given
        String email = "test@test.com";
        User user = User.builder()
                .email(email)
                .status(status)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        //when, then
        assertDoesNotThrow(() -> userService.checkEmailDuplication(email));
    }

}
