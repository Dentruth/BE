package com.dentruth.user.presentation;

import com.dentruth.common.jwt.CustomUserDetails;
import com.dentruth.common.response.ApiResponse;
import com.dentruth.common.response.code.SuccessStatus;
import com.dentruth.user.application.UserService;
import com.dentruth.user.application.dto.response.UserInfoResponse;
import com.dentruth.user.presentation.dto.request.UpdateUserInfoRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserV1Controller {

    private final UserService userService;

    @GetMapping("/email/check")
    public ApiResponse<String> checkEmailDuplication(@RequestParam String email) {
        userService.checkEmailDuplication(email);
        return ApiResponse.onSuccess(SuccessStatus.OK, "사용 가능");
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(SuccessStatus.OK,
                userService.getUserInfo(UUID.fromString(userDetails.getUserId())));
    }

    @PutMapping("/me")
    public ApiResponse<UserInfoResponse> updateUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid @RequestBody UpdateUserInfoRequest request) {
        return ApiResponse.onSuccess(SuccessStatus.OK,
                userService.updateUserInfo(UUID.fromString(userDetails.getUserId()), request.toApplicationRequest()));
    }

}
