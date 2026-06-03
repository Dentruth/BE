package com.dentruth.user.presentation.dto.request;

import com.dentruth.user.application.dto.request.WithdrawnApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class WithdrawnRequest {

    @NotNull(message = "Please enter your password")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "Password must include letters, numbers, and special characters"
    )
    private String password;

    public WithdrawnApplicationRequest toApplicationRequest(){
        return WithdrawnApplicationRequest.builder()
                .password(this.password)
                .build();
    }

}
