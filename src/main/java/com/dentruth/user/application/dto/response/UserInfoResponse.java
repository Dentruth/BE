package com.dentruth.user.application.dto.response;

import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Language;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponse {

    private final String name;
    private final String language;
    private final LocalDate birth;
    private final String gender;
    private final String region;
    private final String stayDuration;
    private final String insuranceStatus;
    private final String nationality;
    private final String accessToken;

    public static UserInfoResponse from(User user, String accessToken) {
        Language lang = user.getLanguage();

        boolean isKorean = (lang == Language.KOREAN);

        return UserInfoResponse.builder()
                .name(user.getName())
                .language(lang.name())
                .birth(user.getBirth())
                .gender(isKorean ? user.getGender().getKo() : user.getGender().getEng())
                .region(user.getRegion())
                .stayDuration(isKorean ? user.getStayDuration().getKo() : user.getStayDuration().getEng())
                .insuranceStatus(isKorean ? user.getInsuranceStatus().getKo() : user.getInsuranceStatus().getEng())
                .nationality(user.getNationality())
                .accessToken(accessToken)
                .build();
    }

}
