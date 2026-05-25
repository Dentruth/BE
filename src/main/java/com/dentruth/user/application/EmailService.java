package com.dentruth.user.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.application.dto.request.SendVerifyEmailApplicationRequest;
import com.dentruth.user.application.dto.request.VerifyEmailApplicationRequest;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailAuthCodeStore emailAuthCodeStore;

    @Value("${email.google.email}")
    private String adminEmail;

    @Value("${email.google.password}")
    private String adminPassword;

    private Session mailSession;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        this.mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(adminEmail, adminPassword);
            }
        });
    }

    public void sendVerifyEmail(SendVerifyEmailApplicationRequest request) {
        String authCode = authRandomCode();

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(adminEmail, "Dentruth"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getEmail()));
            message.setSubject("[Dentruth] Email Verification Request");
            message.setText(buildEmailBody(authCode));

            Transport.send(message);

            emailAuthCodeStore.save(request.getEmail(), authCode);
            log.info("인증 메일 발송 완료. email: {}", SecurityUtils.convertToMaskedEmail(request.getEmail()));
        } catch (Exception e) {
            log.error("이메일 발송 실패. email: {}", SecurityUtils.convertToMaskedEmail(request.getEmail()), e);
            throw new DentruthException(ErrorStatus.EMAIL_SEND_FAILED);
        }

    }

    private String authRandomCode() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            if (Math.random() < 0.5) {
                sb.append((char) ((int) (Math.random() * 10) + '0'));
            } else {
                sb.append((char) ((int) (Math.random() * 26) + 'A'));
            }
        }
        return sb.toString();
    }

    private String buildEmailBody(String authCode) {
        return "Hello,\n\n"
                + "Thank you for registering with Dentruth.\n"
                + "Your verification code is: " + authCode + "\n\n"
                + "Please enter this code to complete your verification.\n"
                + "If you did not request this code, please ignore this email.\n\n"
                + "Best regards,\n"
                + "The Dentruth Team";
    }

    public void verifyEmail(VerifyEmailApplicationRequest request) {
        String authCode = emailAuthCodeStore.findByEmail(request.getEmail())
                .orElseThrow(() -> new DentruthException(ErrorStatus.INVALID_AUTH_CODE));

        if (!request.getAuthCode().equals(authCode)) {
            throw new DentruthException(ErrorStatus.INVALID_AUTH_CODE);
        } else {
            emailAuthCodeStore.deleteByEmail(request.getEmail());
        }
    }

}
