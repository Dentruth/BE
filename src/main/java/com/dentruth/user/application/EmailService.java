package com.dentruth.user.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.SendVerifyEmailApplicationRequest;
import com.dentruth.user.application.dto.request.VerifyEmailApplicationRequest;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailAuthCodeStore emailAuthCodeStore;
    private final EmailSender emailSender;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendVerifyEmail(SendVerifyEmailApplicationRequest request) {
        String authCode = generateAuthCode();
        emailSender.send(request.getEmail(), authCode);
        emailAuthCodeStore.save(request.getEmail(), authCode);
    }

    public void verifyEmail(VerifyEmailApplicationRequest request) {
        String authCode = emailAuthCodeStore.findByEmail(request.getEmail())
                .orElseThrow(() -> new DentruthException(ErrorStatus.INVALID_AUTH_CODE));

        if (!request.getAuthCode().equals(authCode)) {
            throw new DentruthException(ErrorStatus.INVALID_AUTH_CODE);
        }

        emailAuthCodeStore.deleteByEmail(request.getEmail());
    }

    private String generateAuthCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (SECURE_RANDOM.nextBoolean()) {
                sb.append((char) (SECURE_RANDOM.nextInt(10) + '0'));
            } else {
                sb.append((char) (SECURE_RANDOM.nextInt(26) + 'A'));
            }
        }
        return sb.toString();
    }

}
