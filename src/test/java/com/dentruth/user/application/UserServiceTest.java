package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("유저 정보가 존재하면 유저를 찾을 수 있다.")
    @Test
    void shouldFindUser_whenUserExists() {
        //given
        String email = "test@test.com";

        User user = mock(User.class);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        //when, then
        assertThat(userService.findUserByEmail(anyString(), email)).isEqualTo(user);
    }

    @DisplayName("유저가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotExist() {
        //given
        String email = "test@test.com";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(()->userService.findUserByEmail(anyString(), email))
                .isInstanceOf(DentruthException.class)
                .hasMessage(ErrorStatus.USER_NOT_FOUND.getMessage());
    }

}
