package com.dentruth.common.event;

import com.dentruth.config.oauth.user.OAuth2Provider;

public record OAuth2LoginEvent(
        String email,
        String name,
        OAuth2Provider provider
) {}
