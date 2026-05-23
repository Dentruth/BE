package com.dentruth.user.application.dto.request;

import com.dentruth.common.validation.ValidEnum;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SignupApplicationRequest {

    private String email;
    private String password;
    private String name;
    private Language language;
    private LocalDate birthDate;
    private Gender gender;
    private String residentialArea;
    private StayDuration stayDuration;
    private InsuranceStatus insuranceStatus;

}
