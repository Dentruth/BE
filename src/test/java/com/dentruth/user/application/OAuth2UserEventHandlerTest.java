package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dentruth.common.event.OAuth2LoginRequestEvent;
import com.dentruth.common.event.OAuth2SaveTokenEvent;
import com.dentruth.common.event.OAuth2UnlinkRequestEvent;
import com.dentruth.config.oauth.user.OAuth2Provider;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuth2UserEventHandlerTest {

    @InjectMocks
    private OAuth2UserEventHandler handler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Nested
    @DisplayName("handleLogin - 소셜 로그인 처리")
    class HandleLogin {

        @DisplayName("기존 유저가 소셜 로그인하면 이벤트에 기존 userId와 status가 세팅된다.")
        @Test
        void shouldSetExistingUserIdAndStatus_whenExistingUserLogsIn() {
            //given
            UUID userId = UUID.randomUUID();
            User existingUser = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .status(UserStatus.ACTIVE)
                    .userType(UserType.GOOGLE)
                    .build();

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(existingUser));

            OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(
                    "test@test.com", "홍길동", OAuth2Provider.GOOGLE
            );

            //when
            handler.handleLogin(event);

            //then
            assertThat(event.isHandled()).isTrue();
            assertThat(event.getUserId()).isEqualTo(userId.toString());
            assertThat(event.getUserStatus()).isEqualTo("ACTIVE");
            verify(userRepository, never()).save(any());
        }

        @DisplayName("기존 유저가 GUEST 상태이면 이벤트에 GUEST status가 세팅된다.")
        @Test
        void shouldSetGuestStatus_whenExistingUserIsGuest() {
            //given
            UUID userId = UUID.randomUUID();
            User guestUser = User.builder()
                    .id(userId)
                    .email("guest@test.com")
                    .status(UserStatus.GUEST)
                    .userType(UserType.GOOGLE)
                    .build();

            given(userRepository.findByEmail("guest@test.com")).willReturn(Optional.of(guestUser));

            OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(
                    "guest@test.com", "게스트", OAuth2Provider.GOOGLE
            );

            //when
            handler.handleLogin(event);

            //then
            assertThat(event.isHandled()).isTrue();
            assertThat(event.getUserStatus()).isEqualTo("GUEST");
        }

        @DisplayName("신규 유저가 소셜 로그인하면 GUEST 상태로 유저가 생성되고 이벤트에 결과가 세팅된다.")
        @Test
        void shouldCreateGuestUserAndSetResult_whenNewUserLogsIn() {
            //given
            given(userRepository.findByEmail("new@test.com")).willReturn(Optional.empty());

            OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(
                    "new@test.com", "신규유저", OAuth2Provider.GOOGLE
            );

            //when
            handler.handleLogin(event);

            //then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("new@test.com");
            assertThat(savedUser.getName()).isEqualTo("신규유저");
            assertThat(savedUser.getUserType()).isEqualTo(UserType.GOOGLE);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.GUEST);

            assertThat(event.isHandled()).isTrue();
            assertThat(event.getUserId()).isNotNull();
            assertThat(event.getUserStatus()).isEqualTo("GUEST");
        }

        @DisplayName("신규 유저의 userId는 매번 새로 생성된다.")
        @Test
        void shouldGenerateNewUserId_whenNewUserLogsIn() {
            //given
            given(userRepository.findByEmail("a@test.com")).willReturn(Optional.empty());
            given(userRepository.findByEmail("b@test.com")).willReturn(Optional.empty());

            OAuth2LoginRequestEvent eventA = new OAuth2LoginRequestEvent("a@test.com", "A", OAuth2Provider.GOOGLE);
            OAuth2LoginRequestEvent eventB = new OAuth2LoginRequestEvent("b@test.com", "B", OAuth2Provider.GOOGLE);

            //when
            handler.handleLogin(eventA);
            handler.handleLogin(eventB);

            //then
            assertThat(eventA.getUserId()).isNotEqualTo(eventB.getUserId());
        }
    }

    @Nested
    @DisplayName("handleUnlink - 소셜 연동 해제")
    class HandleUnlink {

        @DisplayName("연동 해제 시 RefreshToken이 삭제되고 유저 상태가 WITHDRAWN으로 변경된다.")
        @Test
        void shouldDeleteRefreshTokenAndWithdrawUser_whenUnlinkRequested() {
            //given
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .status(UserStatus.ACTIVE)
                    .userType(UserType.GOOGLE)
                    .build();

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

            OAuth2UnlinkRequestEvent event = new OAuth2UnlinkRequestEvent("test@test.com");

            //when
            handler.handleUnlink(event);

            //then
            verify(tokenService).deleteRefreshToken(userId);
            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(event.isHandled()).isTrue();
            assertThat(event.getUserId()).isEqualTo(userId.toString());
        }

        @DisplayName("존재하지 않는 이메일로 연동 해제 요청 시 아무 처리도 하지 않는다.")
        @Test
        void shouldDoNothing_whenUserNotFoundForUnlink() {
            //given
            given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

            OAuth2UnlinkRequestEvent event = new OAuth2UnlinkRequestEvent("notfound@test.com");

            //when
            handler.handleUnlink(event);

            //then
            verify(tokenService, never()).deleteRefreshToken(any());
            assertThat(event.isHandled()).isFalse();
        }
    }

    @Nested
    @DisplayName("handleSaveToken - Refresh Token 저장")
    class HandleSaveToken {

        @DisplayName("OAuth2SaveTokenEvent를 받으면 TokenService에 RefreshToken을 저장한다.")
        @Test
        void shouldSaveRefreshToken_whenSaveTokenEventReceived() {
            //given
            UUID userId = UUID.randomUUID();
            String refreshToken = "refresh-token-value";

            OAuth2SaveTokenEvent event = new OAuth2SaveTokenEvent(userId.toString(), refreshToken);

            //when
            handler.handleSaveToken(event);

            //then
            verify(tokenService).saveRefreshToken(userId, refreshToken);
        }
    }
}