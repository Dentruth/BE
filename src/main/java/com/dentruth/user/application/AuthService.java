package com.dentruth.user.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.user.application.dto.request.SignupApplicationRequest;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupApplicationRequest request) {
        UUID userId = UUID.randomUUID();
        log.info("회원가입을 진행합니다. id : {}", userId);

        validSignUp(request);

        String encodePassword = passwordEncoder.encode(request.getPassword());

        User user = User.localSignupUser(userId, request.getEmail(), request.getRegion(), request.getNationality(),
                encodePassword, request.getName(), request.getBirthDate(), request.getGender(), request.getLanguage(),
                request.getStayDuration(), request.getInsuranceStatus());

        userRepository.save(user);
    }

    private void validSignUp(SignupApplicationRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            UserStatus status = user.getStatus();

            if (status != UserStatus.DELETED && status != UserStatus.WITHDRAWN) {
                log.info("이미 가입된 이메일로 회원가입 요청. email : {}, userStatus : {}", request.getEmail(), status);
                throw new DentruthException(ErrorStatus.ALREADY_REGISTERED_EMAIL);
            }
        });
    }

    public void verifyPassword(String inputPassword, String storedPassword) {
        if (!passwordEncoder.matches(inputPassword, storedPassword)) {
            throw new DentruthException(ErrorStatus.WRONG_PASSWORD);
        }
    }

    @Transactional
    public void updatePassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
    }

}
