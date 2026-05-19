package com.dentruth.user.presentation;

import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.user.application.AuthService;
import com.dentruth.user.presentation.dto.request.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthV1Controller {

    private final AuthService authService;

    @PostMapping("/signup/local")
    @Operation(summary = "로컬 회원가입")
    public ResponseEntity<ApiResponse<?>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest.toApplicationRequest());
        return ResponseEntity.created(null).body(ApiResponse.onSuccess(SuccessStatus.CREATED, null));
    }

}
