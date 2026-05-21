package com.dentruth.user.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.user.application.AuthFacade;
import com.dentruth.user.application.AuthService;
import com.dentruth.user.presentation.dto.request.LoginRequest;
import com.dentruth.user.presentation.dto.request.SignupRequest;
import com.dentruth.user.presentation.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthV1Controller {

    private final AuthFacade authFacade;
    private final AuthService authService;

    @PostMapping("/signup/local")
    @Operation(summary = "로컬 회원가입")
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest.toApplicationRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.onSuccess(SuccessStatus.CREATED, null));
    }

    @PostMapping("/login")
    @Operation(summary = "로컬 로그인")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.onSuccess(SuccessStatus.OK, authFacade.login(loginRequest.toApplicationRequest()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails){
        UUID userId = UUID.fromString(userDetails.getUserId());
        authFacade.logout(userId);
        return ResponseEntity.noContent().build();
    }

}
