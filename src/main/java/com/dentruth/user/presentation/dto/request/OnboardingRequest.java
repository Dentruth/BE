package com.dentruth.user.presentation.dto.request;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.validation.ValidEnum;
import com.dentruth.user.application.dto.request.OnboardingApplicationRequest;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class OnboardingRequest {

    @NotNull(message = "Please enter your name")
    @Size(min = 2, max = 50, message = "Name cannot exceed 50 characters")
    private String name;

    @NotNull(message = "Please select a language")
    @ValidEnum(enumClass = Language.class, message = "유효한 언어 선택이 아닙니다.")
    private String language;

    @NotNull(message = "Please select your date of birth")
    private LocalDate birthDate;

    @NotNull(message = "Please select your gender")
    @ValidEnum(enumClass = Gender.class, message = "유효한 성별이 아닙니다.")
    private String gender;

    @NotBlank(message = "Please select your region")
    private String region;

    @NotBlank(message = "Please select your nationality")
    private String nationality;

    @NotNull(message = "Please select your duration of stay")
    @ValidEnum(enumClass = StayDuration.class, message = "유효한 체류기간 형식이 아닙니다.")
    private String stayDuration;

    @NotNull(message = "Please select your insurance status")
    @ValidEnum(enumClass = InsuranceStatus.class, message = "유효한 보험 여부 형식이 아닙니다.")
    private String insuranceStatus;

    public OnboardingApplicationRequest toApplicationRequest(){
        return OnboardingApplicationRequest.builder()
                .name(this.name)
                .language(Language.valueOf(this.language.toUpperCase()))
                .birthDate(this.birthDate)
                .gender(Gender.valueOf(this.gender.toUpperCase()))
                .region(this.region)
                .nationality(this.nationality)
                .stayDuration(StayDuration.valueOf(this.stayDuration.toUpperCase()))
                .insuranceStatus(InsuranceStatus.valueOf(this.insuranceStatus.toUpperCase()))
                .build();
    }

}
