package com.dentruth.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.domain.entity.enums.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

}
