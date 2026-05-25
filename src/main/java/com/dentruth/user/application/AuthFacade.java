package com.dentruth.user.application;

import com.dentruth.common.exception.JwtAuthenticationException;
import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.application.dto.request.LoginApplicationRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.application.dto.response.TokenResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final TokenService tokenService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public TokenResponse login(LoginApplicationRequest request) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(request.getEmail());
        log.info("로그인 요청. Email : [{}]", maskedEmail);

        User user = userService.findValidUserByEmail("로그인", request.getEmail());
        user.validateStatus();
        authService.verifyPassword(request.getPassword(), user.getPassword());

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString(), user.getLanguage().toString());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        tokenService.saveRefreshToken(user.getId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(UUID userId) {
        tokenService.deleteRefreshToken(userId);
    }

    public TokenResponse reissue(String refreshToken) {
        jwtProvider.validateRefreshToken(refreshToken);

        String userIdStr = jwtProvider.getUserId(refreshToken);
        UUID userId = UUID.fromString(userIdStr);
        User user = userService.findById(userId, "토큰 재발급");

        String storedRefreshToken = tokenService.getRefreshToken(userId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            log.warn("토큰 재발급 실패. Redis에 저장된 토큰과 일치하지 않거나 이미 로그아웃된 유저. User Id : [{}]", userId);
            throw new JwtAuthenticationException(ErrorStatus.INVALID_TOKEN);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userIdStr, user.getLanguage().toString());
        String newRefreshToken = jwtProvider.generateRefreshToken(userIdStr);

        tokenService.saveRefreshToken(userId, newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

}
