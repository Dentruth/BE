package com.dentruth.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendVerifyEmailApplicationRequest {

    private String email;

}
