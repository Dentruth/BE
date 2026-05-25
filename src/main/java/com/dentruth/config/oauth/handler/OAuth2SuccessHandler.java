package com.dentruth.config.oauth.handler;

import com.dentruth.common.event.OAuth2LoginRequestEvent;
import com.dentruth.common.event.OAuth2UnlinkRequestEvent;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.common.util.CookieUtil;
import com.dentruth.config.oauth.OAuth2UserPrincipal;
import com.dentruth.config.oauth.OAuthCookieRepository;
import com.dentruth.common.domain.enums.Language;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final OAuthCookieRepository cookieRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);
        if (response.isCommitted()) {
            return;
        }
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String targetUrl = CookieUtil.getCookie(request, OAuthCookieRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(getDefaultTargetUrl());

        String mode = CookieUtil.getCookie(request, OAuthCookieRepository.MODE_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("login");

        OAuth2UserPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            return buildErrorUrl(targetUrl, "Login failed");
        }

        if ("login".equalsIgnoreCase(mode)) {
            return handleLogin(response, targetUrl, principal);
        } else if ("unlink".equalsIgnoreCase(mode)) {
            return handleUnlink(request, response, targetUrl, principal);
        }

        return buildErrorUrl(targetUrl, "Unknown mode");
    }

    private String handleLogin(HttpServletResponse response,
                               String targetUrl, OAuth2UserPrincipal principal) {
        OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(
                principal.getUserInfo().getEmail(),
                principal.getUserInfo().getName(),
                principal.getUserInfo().getProvider()
        );
        eventPublisher.publishEvent(event);

        if (!event.isHandled()) {
            log.error("OAuth2 로그인 이벤트가 처리되지 않았습니다. email: [{}]", principal.getUserInfo().getEmail());
            return buildErrorUrl(targetUrl, "Login processing failed");
        }

        String accessToken = jwtProvider.generateAccessToken(event.getUserId(), Language.ENGLISH.name());
        String refreshToken = jwtProvider.generateRefreshToken(event.getUserId());

        com.dentruth.common.event.OAuth2SaveTokenEvent saveTokenEvent =
                new com.dentruth.common.event.OAuth2SaveTokenEvent(event.getUserId(), refreshToken);
        eventPublisher.publishEvent(saveTokenEvent);

        CookieUtil.addCookie(response, "access_token", accessToken,
                (int) Duration.ofDays(1).toSeconds());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("access_token", accessToken)
                .queryParam("is_new_user", event.isNewUser())
                .build().toUriString();
    }

    private String handleUnlink(HttpServletRequest request, HttpServletResponse response,
                                String targetUrl, OAuth2UserPrincipal principal) {
        OAuth2UnlinkRequestEvent event = new OAuth2UnlinkRequestEvent(
                principal.getUserInfo().getEmail()
        );
        eventPublisher.publishEvent(event);

        CookieUtil.deleteCookie(request, response, "access_token");
        return UriComponentsBuilder.fromUriString(targetUrl).build().toUriString();
    }

    private OAuth2UserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2UserPrincipal p) {
            return p;
        }
        return null;
    }

    private String buildErrorUrl(String targetUrl, String message) {
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", message)
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieRepository.removeAuthorizationRequestCookies(request, response);
    }

}
