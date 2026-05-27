package com.dentruth.config.oauth.port;

import com.dentruth.config.oauth.user.OAuth2Provider;

public interface OAuth2UserPort {
    OAuth2LoginResult processLogin(String email, String name, OAuth2Provider provider);

    void processUnlink(String email);
}
