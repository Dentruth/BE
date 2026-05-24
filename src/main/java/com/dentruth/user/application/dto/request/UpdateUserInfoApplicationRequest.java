package com.dentruth.user.application.dto.request;

import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UpdateUserInfoApplicationRequest {

    private String name;
    private Language language;
    private LocalDate birthDate;
    private Gender gender;
    private String region;
    private String nationality;
    private StayDuration stayDuration;
    private InsuranceStatus insuranceStatus;

}
