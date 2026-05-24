package com.dentruth.user.application;

import com.dentruth.user.application.dto.request.UpdatePasswordApplicationRequest;
import com.dentruth.user.application.dto.request.WithdrawnApplicationRequest;
import com.dentruth.user.domain.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final AuthService authService;

    @Transactional
    public void deleteUser(UUID userId, WithdrawnApplicationRequest request) {
        log.info("유저 탈퇴 요청. User Id : [{}]", userId);
        User user = userService.findById(userId, "탈퇴");
        user.validateStatus();

        authService.verifyPassword(request.getPassword(), user.getPassword());

        user.withdrawn();
    }

    public void updatePassword(UUID userId, UpdatePasswordApplicationRequest request) {
        log.info("유저 비밀번호 변경 요청. User Id : [{}]", userId);
        User user = userService.findById(userId, "비밀번호 변경");
        user.validateStatus();

        authService.verifyPassword(request.getExistingPassword(), user.getPassword());
        authService.updatePassword(user, request.getNewPassword());
    }

}
