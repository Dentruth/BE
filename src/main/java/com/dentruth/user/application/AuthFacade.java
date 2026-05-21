package com.dentruth.user.application;

import com.dentruth.common.jwt.JwtProvider;
import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.application.dto.request.LoginApplicationRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.presentation.dto.response.LoginResponse;
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

    public LoginResponse login(LoginApplicationRequest request) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(request.getEmail());
        log.info("로그인 요청. Email : [{}]", maskedEmail);

        User user = userService.findValidUserByEmail("로그인", request.getEmail());
        user.validateStatus();
        authService.verifyPassword(request.getPassword(), user.getPassword());

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        tokenService.saveRefreshToken(user.getId(), refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(UUID userId) {
        tokenService.deleteRefreshToken(userId);
    }

}
