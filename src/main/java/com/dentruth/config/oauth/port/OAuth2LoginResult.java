package com.dentruth.config.oauth.port;

public record OAuth2LoginResult(
        String userId,
        boolean isNewUser
) {}
