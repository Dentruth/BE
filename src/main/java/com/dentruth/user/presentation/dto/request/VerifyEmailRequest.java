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

    @NotNull(message = "이메일은 필수 입력입니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "유효한 이메일 형식이 아닙니다."
    )
    private String email;

    @NotBlank(message = "인증코드는 필수 입력입니다.")
    private String authCode;

    public VerifyEmailApplicationRequest toApplicationRequest(){
        return VerifyEmailApplicationRequest.builder()
                .email(this.email)
                .authCode(this.authCode)
                .build();
    }

}
