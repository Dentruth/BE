package com.dentruth.user.infra.redis;

import com.dentruth.common.util.SecurityUtils;
import com.dentruth.user.application.EmailAuthCodeStore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisEmailAuthCodeRepository implements EmailAuthCodeStore {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "auth:email:";
    private static final long REDIS_TTL_MINUTES = 10L;
    private static final long AUTH_VALID_MINUTES = 5L;

    @Override
    public void save(String email, String authCode) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);
        String key = KEY_PREFIX + email;
        long now = System.currentTimeMillis();

        String value = authCode + ":" + now;

        log.info("이메일 인증 코드 레디스 저장. 이메일 : [{}], TTL : [{}분]", maskedEmail, REDIS_TTL_MINUTES);

        redisTemplate.opsForValue().set(key, value, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
    }

}
