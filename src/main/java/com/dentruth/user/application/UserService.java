package com.dentruth.user.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findUserByEmail(String method, String email) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. Email : [{}]", method, maskedEmail);
                    return new DentruthException(ErrorStatus.USER_NOT_FOUND);
                });
    }

    @Transactional(readOnly = true)
    public void checkEmailDuplication(String email) {
        userRepository.findByEmail(email)
                .ifPresent(User::validateDuplicationEmailByStatus);
    }

}
