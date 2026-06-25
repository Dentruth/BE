package com.dentruth.config.oauth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dentruth.common.event.OAuth2LoginRequestEvent;
import com.dentruth.common.event.OAuth2SaveTokenEvent;
import com.dentruth.common.event.OAuth2UnlinkRequestEvent;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.config.oauth.OAuth2UserPrincipal;
import com.dentruth.config.oauth.OAuthCookieRepository;
import com.dentruth.config.oauth.user.GoogleUserInfo;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private OAuthCookieRepository cookieRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(successHandler, "defaultRedirectUri", REDIRECT_URI);
    }

    /** redirect_uri, mode 쿠키를 request에 심어주는 헬퍼 */
    private void addCookies(String redirectUri, String mode) {
        request.setCookies(
                new Cookie(OAuthCookieRepository.REDIRECT_URI_PARAM_COOKIE_NAME, redirectUri),
                new Cookie(OAuthCookieRepository.MODE_PARAM_COOKIE_NAME, mode)
        );
    }

    private OAuth2UserPrincipal createPrincipal(String email, String name) {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-id",
                "email", email,
                "name", name,
                "email_verified", true
        );
        GoogleUserInfo userInfo = new GoogleUserInfo(attributes, "google-access-token");
        return new OAuth2UserPrincipal(userInfo);
    }

    private void stubLoginEvent(String userId, String userStatus) {
        willAnswer(invocation -> {
            Object event = invocation.getArgument(0);
            if (event instanceof OAuth2LoginRequestEvent loginEvent) {
                loginEvent.setResult(userId, userStatus);
            }
            return null;
        }).given(eventPublisher).publishEvent(any(OAuth2LoginRequestEvent.class));
    }

    @Nested
    @DisplayName("login 모드")
    class LoginMode {

        @DisplayName("신규 유저(GUEST) 로그인 시 리다이렉트 URL에 access_token과 status=GUEST가 포함된다.")
        @Test
        void shouldRedirectWithGuestStatus_whenNewUserLogsIn() {
            //given
            addCookies(REDIRECT_URI, "login");
            OAuth2UserPrincipal principal = createPrincipal("new@test.com", "신규유저");
            given(authentication.getPrincipal()).willReturn(principal);

            stubLoginEvent(USER_ID, "GUEST");
            given(jwtProvider.generateAccessToken(eq(USER_ID), anyString())).willReturn(ACCESS_TOKEN);
            given(jwtProvider.generateRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("access_token=" + ACCESS_TOKEN);
            assertThat(targetUrl).contains("status=GUEST");
            assertThat(targetUrl).startsWith(REDIRECT_URI);
        }

        @DisplayName("기존 유저(ACTIVE) 로그인 시 리다이렉트 URL에 status=ACTIVE가 포함된다.")
        @Test
        void shouldRedirectWithActiveStatus_whenExistingUserLogsIn() {
            //given
            addCookies(REDIRECT_URI, "login");
            OAuth2UserPrincipal principal = createPrincipal("existing@test.com", "기존유저");
            given(authentication.getPrincipal()).willReturn(principal);

            stubLoginEvent(USER_ID, "ACTIVE");
            given(jwtProvider.generateAccessToken(eq(USER_ID), anyString())).willReturn(ACCESS_TOKEN);
            given(jwtProvider.generateRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("status=ACTIVE");
        }

        @DisplayName("로그인 성공 시 OAuth2SaveTokenEvent가 발행된다.")
        @Test
        void shouldPublishSaveTokenEvent_whenLoginSucceeds() {
            //given
            addCookies(REDIRECT_URI, "login");
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);

            stubLoginEvent(USER_ID, "ACTIVE");
            given(jwtProvider.generateAccessToken(eq(USER_ID), anyString())).willReturn(ACCESS_TOKEN);
            given(jwtProvider.generateRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);

            //when
            successHandler.determineTargetUrl(request, response, authentication);

            //then
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());

            boolean hasSaveTokenEvent = eventCaptor.getAllValues().stream()
                    .anyMatch(e -> e instanceof OAuth2SaveTokenEvent saveEvent
                            && saveEvent.userId().equals(USER_ID)
                            && saveEvent.refreshToken().equals(REFRESH_TOKEN));
            assertThat(hasSaveTokenEvent).isTrue();
        }

        @DisplayName("이벤트 핸들러가 처리하지 않으면(isHandled=false) 에러 URL로 리다이렉트된다.")
        @Test
        void shouldRedirectToErrorUrl_whenEventNotHandled() {
            //given
            addCookies(REDIRECT_URI, "login");
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);
            // publishEvent를 아무것도 안 하면 event.isHandled() == false

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("error=");
            assertThat(targetUrl).doesNotContain("access_token");
        }

        @DisplayName("principal이 null이면 에러 URL로 리다이렉트된다.")
        @Test
        void shouldRedirectToErrorUrl_whenPrincipalIsNull() {
            //given
            addCookies(REDIRECT_URI, "login");
            given(authentication.getPrincipal()).willReturn("not-oauth2-principal");

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("error=login_failed");
            verify(eventPublisher, never()).publishEvent(any());
        }

        @DisplayName("redirect_uri 쿠키가 없으면 defaultRedirectUri로 리다이렉트된다.")
        @Test
        void shouldUseDefaultRedirectUri_whenCookieNotPresent() {
            //given
            // 쿠키 없이 요청
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);

            stubLoginEvent(USER_ID, "ACTIVE");
            given(jwtProvider.generateAccessToken(eq(USER_ID), anyString())).willReturn(ACCESS_TOKEN);
            given(jwtProvider.generateRefreshToken(USER_ID)).willReturn(REFRESH_TOKEN);

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).startsWith(REDIRECT_URI);
        }
    }

    @Nested
    @DisplayName("unlink 모드")
    class UnlinkMode {

        @DisplayName("연동 해제 시 OAuth2UnlinkRequestEvent가 발행된다.")
        @Test
        void shouldPublishUnlinkEvent_whenUnlinkRequested() {
            //given
            addCookies(REDIRECT_URI, "unlink");
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);

            //when
            successHandler.determineTargetUrl(request, response, authentication);

            //then
            ArgumentCaptor<OAuth2UnlinkRequestEvent> captor =
                    ArgumentCaptor.forClass(OAuth2UnlinkRequestEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("test@test.com");
        }

        @DisplayName("연동 해제 시 리다이렉트 URL에 status=unlinked가 포함된다.")
        @Test
        void shouldRedirectWithUnlinkedStatus_whenUnlinkSucceeds() {
            //given
            addCookies(REDIRECT_URI, "unlink");
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("status=unlinked");
            assertThat(targetUrl).startsWith(REDIRECT_URI);
        }
    }

    @Nested
    @DisplayName("알 수 없는 mode")
    class UnknownMode {

        @DisplayName("알 수 없는 mode이면 에러 URL로 리다이렉트된다.")
        @Test
        void shouldRedirectToErrorUrl_whenModeIsUnknown() {
            //given
            addCookies(REDIRECT_URI, "unknown_mode");
            OAuth2UserPrincipal principal = createPrincipal("test@test.com", "테스트유저");
            given(authentication.getPrincipal()).willReturn(principal);

            //when
            String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

            //then
            assertThat(targetUrl).contains("error=unknown_mode");
        }
    }

}
