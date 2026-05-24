package com.dentruth.user.application;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.application.dto.request.UpdateUserInfoApplicationRequest;
import com.dentruth.user.application.dto.response.UserInfoResponse;
import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import com.dentruth.user.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
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
    public User findValidUserByEmail(String method, String email) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);

        return userRepository.findByEmailAndStatusIn(email,
                        List.of(UserStatus.ACTIVE, UserStatus.GUEST, UserStatus.SUSPENDED, UserStatus.BLOCKED))
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. Email : [{}]", method, maskedEmail);
                    return new DentruthException(ErrorStatus.USER_NOT_FOUND);
                });
    }

    @Transactional(readOnly = true)
    public void checkEmailDuplication(String email) {
        userRepository.findByEmailAndStatusIn(email,
                        List.of(UserStatus.ACTIVE, UserStatus.GUEST, UserStatus.SUSPENDED, UserStatus.BLOCKED))
                .ifPresent(User::validateDuplicationEmailByStatus);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(UUID userId) {
        User user = findById(userId, "유저 정보 조회");
        user.validateStatus();

        return UserInfoResponse.from(user);
    }

    @Transactional
    public UserInfoResponse updateUserInfo(UUID userId, UpdateUserInfoApplicationRequest request) {
        log.info("유저 정보 업데이트 요청. User Id : [{}], 업데이트 요청 정보 : [{}]", userId, request.toString());
        User user = findById(userId, "유저 정보 업데이트");
        user.validateStatus();

        user.updateInfo(request.getName(), request.getLanguage(), request.getBirthDate(), request.getGender(),
                request.getRegion(), request.getStayDuration(), request.getInsuranceStatus(), request.getNationality());

        return UserInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User findById(UUID userId, String method) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. User Id : [{}]", method, userId);
                    return new DentruthException(ErrorStatus.USER_NOT_FOUND);
                });
    }

}
