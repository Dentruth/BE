package com.dentruth.user.presentation.dto.request;

import com.dentruth.common.validation.ValidEnum;
import com.dentruth.user.application.dto.request.SignupApplicationRequest;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class SignupRequest {

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

    @NotNull(message = "이름은 필수 입력입니다.")
    @Size(min = 2, max = 20, message = "이름은 2~20자 사이여야 합니다.")
    private String name;

    @NotNull(message = "언어 선택은 필수입니다.")
    @ValidEnum(enumClass = Language.class, message = "유효한 언어 선택이 아닙니다.")
    private String language;

    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다.")
    @ValidEnum(enumClass = Gender.class, message = "유효한 성별이 아닙니다.")
    private String gender;

    @NotBlank(message = "거주지역은 필수입니다.")
    private String residentialArea;

    @NotNull(message = "체류기간은 필수입니다.")
    @ValidEnum(enumClass = StayDuration.class, message = "유효한 체류기간 형식이 아닙니다.")
    private String stayDuration;

    @NotNull(message = "보험 여부는 필수입니다.")
    @ValidEnum(enumClass = InsuranceStatus.class, message = "유효한 보험 여부 형식이 아닙니다.")
    private String insuranceStatus;

    public SignupApplicationRequest toApplicationRequest(){
        return SignupApplicationRequest.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .language(Language.valueOf(this.language.toUpperCase()))
                .birthDate(this.birthDate)
                .gender(Gender.valueOf(this.gender.toUpperCase()))
                .residentialArea(this.residentialArea)
                .stayDuration(StayDuration.valueOf(this.stayDuration.toUpperCase()))
                .insuranceStatus(InsuranceStatus.valueOf(this.insuranceStatus.toUpperCase()))
                .build();
    }

}
