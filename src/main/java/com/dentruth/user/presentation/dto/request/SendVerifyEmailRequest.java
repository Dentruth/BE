package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.SendVerifyEmailApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class SendVerifyEmailRequest {

    @NotNull(message = "Please enter your email")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "Please enter a valid email address"
    )
    private String email;

    public SendVerifyEmailApplicationRequest toApplicationRequest(){
        return SendVerifyEmailApplicationRequest.builder()
                .email(this.email)
                .build();
    }

}
