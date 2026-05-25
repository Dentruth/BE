package com.dentruth.user.application;

public interface EmailAuthCodeStore {
    void save(String email, String authCode);
}
