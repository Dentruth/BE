package com.dentruth.user.infra.redis;

import com.dentruth.user.application.TokenStore;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.redis-prefix-refresh}")
    private String refreshKeyPrefix;

    @Override
    public void save(UUID userId, String refreshToken, Duration refreshTokenTtl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, refreshTokenTtl);
    }

    @Override
    public String findByUserId(UUID userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    @Override
    public void delete(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return refreshKeyPrefix + userId.toString();
    }

}
