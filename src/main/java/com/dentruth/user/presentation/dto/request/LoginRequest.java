package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.LoginApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class LoginRequest {

    @NotNull(message = "이메일은 필수 입력입니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "유효한 이메일 형식이 아닙니다."
    )
    private String email;

    @NotNull(message = "비밀번호는 필수 입력입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "유효한 비밀번호 형식이 아닙니다."
    )
    private String password;

    public LoginApplicationRequest toApplicationRequest(){
        return LoginApplicationRequest.builder()
                .email(this.email)
                .password(this.password)
                .build();
    }

}
