package com.dentruth.user.application;

import java.util.Optional;

public interface EmailAuthCodeStore {
    void save(String email, String authCode);

    Optional<String> findByEmail(String email);

    void deleteByEmail(String email);

    void saveVerifiedToken(String email, String verifyToken, int ttl);

    Optional<String> findVerifiedTokenByEmail(String email);

}
