package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.WithdrawnApplicationRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class WithdrawnRequest {

    @NotNull(message = "비밀번호는 필수 입력입니다.")
    private String password;

    public WithdrawnApplicationRequest toApplicationRequest(){
        return WithdrawnApplicationRequest.builder()
                .password(this.password)
                .build();
    }

}
