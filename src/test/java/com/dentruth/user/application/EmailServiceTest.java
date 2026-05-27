package com.dentruth.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.VerifyEmailApplicationRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailAuthCodeStore emailAuthCodeStore;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private EmailService emailService;

    @DisplayName("인증 코드가 일치하면 이메일 인증에 성공한다.")
    @Test
    void shouldSucceedVerification_whenAuthCodeMatches() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "ABC123");
        given(emailAuthCodeStore.findByEmail("test@test.com")).willReturn(Optional.of("ABC123"));

        //when, then
        assertThatNoException().isThrownBy(() -> emailService.verifyEmail(request));
        verify(emailAuthCodeStore).deleteByEmail("test@test.com");
    }

    @DisplayName("인증 코드가 불일치하면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeNotMatches() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "WRONG1");
        given(emailAuthCodeStore.findByEmail("test@test.com")).willReturn(Optional.of("ABC123"));

        //when, then
        assertThatThrownBy(() -> emailService.verifyEmail(request))
                .isInstanceOf(DentruthException.class)
                .satisfies(e -> {
                    DentruthException ex = (DentruthException) e;
                    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatus.INVALID_AUTH_CODE);
                });

        verify(emailAuthCodeStore, never()).deleteByEmail(any());
    }

    @DisplayName("인증 코드가 존재하지 않으면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeNotFound() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "ABC123");
        given(emailAuthCodeStore.findByEmail("test@test.com"))
                .willThrow(new DentruthException(ErrorStatus.INVALID_AUTH_CODE));

        //when, then
        assertThatThrownBy(() -> emailService.verifyEmail(request))
                .isInstanceOf(DentruthException.class)
                .satisfies(e -> {
                    DentruthException ex = (DentruthException) e;
                    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatus.INVALID_AUTH_CODE);
                });
    }

}
