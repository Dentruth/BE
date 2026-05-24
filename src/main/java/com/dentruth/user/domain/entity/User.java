package com.dentruth.user.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.Language;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"password", "email", "address"})
@Getter
@Builder
@Slf4j
public class User extends BaseEntity {

    @Id
    private UUID id;

    private String region;
    private String nationality;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    private String name;
    private LocalDate birth;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Language language;

    @Enumerated(EnumType.STRING)
    private StayDuration stayDuration;

    @Enumerated(EnumType.STRING)
    private InsuranceStatus insuranceStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public static User localSignupUser(UUID id, String email, String region, String nationality, String password,
                                       String name, LocalDate birth, Gender gender, Language language,
                                       StayDuration stayDuration, InsuranceStatus insuranceStatus) {
        return User.builder()
                .id(id)
                .region(region)
                .nationality(nationality)
                .email(email)
                .password(password)
                .userType(UserType.LOCAL)
                .name(name)
                .birth(birth)
                .gender(gender)
                .language(language)
                .stayDuration(stayDuration)
                .insuranceStatus(insuranceStatus)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void validateStatus() {
        switch (this.status) {
            case SUSPENDED -> throw new DentruthException(ErrorStatus.SUSPENDED_USER);
            case BLOCKED -> throw new DentruthException(ErrorStatus.BLOCKED_USER);
            case WITHDRAWN, DELETED -> throw new DentruthException(ErrorStatus.USER_NOT_FOUND);
            default -> {
            }
        }
    }

    public void validateDuplicationEmailByStatus() {
        switch (this.status) {
            case ACTIVE, SUSPENDED, BLOCKED, GUEST -> throw new DentruthException(ErrorStatus.ALREADY_REGISTERED_EMAIL);
            case WITHDRAWN, DELETED -> {
            }

            default -> {
            }
        }
    }

    public void updateInfo(String name, Language language, LocalDate birthDate, Gender gender, String region,
                           StayDuration stayDuration, InsuranceStatus insuranceStatus, String nationality) {
        validateName(name);
        validateBirthDate(birthDate);
        validateLocationAndIdentity(region, nationality);
        validateRequiredEnums(language, gender, stayDuration, insuranceStatus);
        
        this.name = name;
        this.language = language;
        this.birth = birthDate;
        this.gender = gender;
        this.region = region;
        this.stayDuration = stayDuration;
        this.insuranceStatus = insuranceStatus;
        this.nationality = nationality;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("도메인 제약 위반: 이름이 비어있음. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST); 
        }
        if (name.length() < 2 || name.length() > 50) {
            log.warn("이름 글자수 제한 위반: [{}]. User Id : [{}]", name, this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            log.warn("도메인 제약 위반: 생년월일이 누락됨. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        
        if (birthDate.isAfter(LocalDate.now())) {
            log.warn("생년월일이 미래 날짜입니다. 입력 날짜 : [{}], User Id : [{}]", birthDate, this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        
        if (birthDate.isBefore(LocalDate.now().minusYears(150))) {
            log.warn("생년월일이 비정상적인 과거 날짜입니다. 입력 날짜 : [{}], User Id : [{}]", birthDate, this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
    }

    private void validateLocationAndIdentity(String region, String nationality) {
        if (region == null || region.trim().isEmpty()) {
            log.warn("도메인 제약 위반: 거주 지역이 비어있음. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        if (nationality == null || nationality.trim().isEmpty()) {
            log.warn("도메인 제약 위반: 국적이 비어있음. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
    }

    private void validateRequiredEnums(Language language, Gender gender,
                                       StayDuration stayDuration, InsuranceStatus insuranceStatus) {
        if (language == null) {
            log.warn("도메인 제약 위반: 선택된 언어가 null입니다. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        if (gender == null) {
            log.warn("도메인 제약 위반: 선택된 성별이 null입니다. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        if (stayDuration == null) {
            log.warn("도메인 제약 위반: 체류 기간이 null입니다. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
        if (insuranceStatus == null) {
            log.warn("도메인 제약 위반: 보험 상태가 null입니다. User Id : [{}]", this.id);
            throw new DentruthException(ErrorStatus.BAD_REQUEST);
        }
    }

}
