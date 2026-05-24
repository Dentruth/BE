package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.UpdatePasswordApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class UpdatePasswordRequest {

    @NotNull(message = "기존 비밀번호는 필수 입력입니다.")
    private String existingPassword;

    @NotNull(message = "변경할 비밀번호는 필수 입력입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "유효한 비밀번호 형식이 아닙니다."
    )
    private String newPassword;

    public UpdatePasswordApplicationRequest toApplicationRequest(){
        return UpdatePasswordApplicationRequest.builder()
                .existingPassword(this.existingPassword)
                .newPassword(this.newPassword)
                .build();
    }

}
