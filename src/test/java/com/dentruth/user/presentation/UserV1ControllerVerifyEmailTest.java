package com.dentruth.user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.EmailService;
import com.dentruth.user.presentation.dto.request.VerifyEmailRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserV1ControllerVerifyEmailTest extends ControllerTestSupport {

    @MockitoBean
    private EmailService emailService;

    @DisplayName("올바른 인증 코드를 입력하면 이메일 인증에 성공한다.")
    @Test
    void shouldSucceedVerification_whenAuthCodeIsValid() throws Exception {
        //given
        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "ABC123");
        doNothing().when(emailService).verifyEmail(any());

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"));
    }

    @DisplayName("인증 코드가 만료되면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeExpired() throws Exception {
        //given
        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "ABC123");
        doThrow(new DentruthException(ErrorStatus.EXPIRED_AUTH_CODE))
                .when(emailService).verifyEmail(any());

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_009"));
    }

    @DisplayName("인증 코드가 일치하지 않으면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeInvalid() throws Exception {
        //given
        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "WRONG1");
        doThrow(new DentruthException(ErrorStatus.INVALID_AUTH_CODE))
                .when(emailService).verifyEmail(any());

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_008"));
    }

}
