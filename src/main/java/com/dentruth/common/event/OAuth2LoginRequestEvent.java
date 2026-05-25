package com.dentruth.common.event;

import com.dentruth.config.oauth.user.OAuth2Provider;

public class OAuth2LoginRequestEvent {

    private final String email;
    private final String name;
    private final OAuth2Provider provider;
    private String userId;
    private boolean isNewUser;
    private boolean handled = false;

    public OAuth2LoginRequestEvent(String email, String name, OAuth2Provider provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    public String getEmail() { return email; }
    public String getName() { return name; }
    public OAuth2Provider getProvider() { return provider; }

    public void setResult(String userId, boolean isNewUser) {
        this.userId = userId;
        this.isNewUser = isNewUser;
        this.handled = true;
    }

    public String getUserId() { return userId; }
    public boolean isNewUser() { return isNewUser; }
    public boolean isHandled() { return handled; }
}
