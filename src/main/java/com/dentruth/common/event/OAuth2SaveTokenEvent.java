package com.dentruth.common.event;

public record OAuth2SaveTokenEvent(
        String userId,
        String refreshToken
) {
}
