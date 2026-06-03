package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.VerifyEmailApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class VerifyEmailRequest {

    @NotNull(message = "Please enter your email")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "Please enter a valid email address"
    )
    private String email;

    @NotBlank(message = "Please enter the verification code")
    private String authCode;

    public VerifyEmailApplicationRequest toApplicationRequest(){
        return VerifyEmailApplicationRequest.builder()
                .email(this.email)
                .authCode(this.authCode)
                .build();
    }

}
