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

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"password", "email", "address"})
@Getter
@Builder
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

}
