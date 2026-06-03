package com.dentruth.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.domain.enums.Language;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.EmailAuthCodeStore;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.Gender;
import com.dentruth.user.domain.entity.enums.InsuranceStatus;
import com.dentruth.user.domain.entity.enums.StayDuration;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.entity.enums.UserType;
import com.dentruth.user.domain.repository.UserRepository;
import com.dentruth.user.presentation.dto.request.ResetPasswordRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthV1ControllerResetPasswordTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailAuthCodeStore emailAuthCodeStore;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("비밀번호 초기화에 성공한다.")
    @Test
    void shouldSucceedResetPassword_successfully() throws Exception {
        //given
        String email = "test@test.com";
        String verifiedToken = UUID.randomUUID().toString();
        String newPassword = "newPassword1234!";

        User user = getUser(UUID.randomUUID(), UserStatus.ACTIVE);
        userRepository.save(user);

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email)).willReturn(verifiedToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .password(newPassword)
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"));

        //then
        User updatedUser = userRepository.findById(user.getId()).get();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        then(emailAuthCodeStore).should(times(1)).deleteVerifiedTokenByEmail(email);
    }

    @DisplayName("존재하지 않는 유저면 비밀번호 초기화에 실패한다.")
    @Test
    void shouldFailResetPassword_whenUserNotFound() throws Exception {
        //given
        String email = "notfound@test.com";
        String verifiedToken = UUID.randomUUID().toString();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email)).willReturn(verifiedToken);

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .password("newPassword1234!")
                .verifiedToken(verifiedToken)
                .build();

        //when, then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        then(emailAuthCodeStore).should(never()).deleteVerifiedTokenByEmail(any());
    }

    @DisplayName("이메일 인증 토큰이 없으면 비밀번호 초기화에 실패한다.")
    @Test
    void shouldFailResetPassword_whenVerifiedTokenNotFound() throws Exception {
        //given
        String email = "test@test.com";

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email)).willReturn("");

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .password("newPassword1234!")
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        //when, then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorStatus.UNAUTHORIZED_EMAIL_VERIFICATION.getMessage()));

        assertThat(userRepository.count()).isEqualTo(0);
        then(emailAuthCodeStore).should(never()).deleteVerifiedTokenByEmail(any());
    }

    @DisplayName("이메일 인증 토큰이 일치하지 않으면 비밀번호 초기화에 실패한다.")
    @Test
    void shouldFailResetPassword_whenVerifiedTokenMismatch() throws Exception {
        //given
        String email = "test@test.com";

        given(emailAuthCodeStore.findVerifiedTokenByEmail(email))
                .willReturn(UUID.randomUUID().toString());

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .password("newPassword1234!")
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        //when, then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorStatus.UNAUTHORIZED_EMAIL_VERIFICATION.getMessage()));

        assertThat(userRepository.count()).isEqualTo(0);
        then(emailAuthCodeStore).should(never()).deleteVerifiedTokenByEmail(any());
    }

    private User getUser(UUID userId, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .region("Region")
                .nationality("nationality")
                .email("test@test.com")
                .password("password12345!")
                .userType(UserType.LOCAL)
                .name("name")
                .birth(LocalDate.of(2002, 5, 24))
                .gender(Gender.F)
                .language(Language.KOREAN)
                .stayDuration(StayDuration.ONE_TO_THREE_M)
                .insuranceStatus(InsuranceStatus.INSURED)
                .status(userStatus)
                .build();
    }

}
