package com.dentruth.user.application.dto.request;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
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
    private String region;
    private String nationality;
    private StayDuration stayDuration;
    private InsuranceStatus insuranceStatus;
    private String verifiedToken;

}
