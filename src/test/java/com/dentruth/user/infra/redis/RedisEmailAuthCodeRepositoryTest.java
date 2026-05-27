package com.dentruth.user.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.dentruth.common.exception.DentruthException;
import com.dentruth.common.response.code.ErrorStatus;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisEmailAuthCodeRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisEmailAuthCodeRepository redisEmailAuthCodeRepository;

    private static final String EMAIL = "test@test.com";
    private static final String AUTH_CODE = "ABC123";
    private static final String KEY_PREFIX = "auth:email:";

    @DisplayName("이메일 인증 코드를 저장할 수 있다.")
    @Test
    void shouldSaveAuthCode_successfully() {
        //given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        //when
        redisEmailAuthCodeRepository.save(EMAIL, AUTH_CODE);

        //then
        verify(valueOperations).set(
                eq(KEY_PREFIX + EMAIL),
                argThat(value -> value.startsWith(AUTH_CODE + ":")),
                eq(10L),
                eq(TimeUnit.MINUTES)
        );
    }

    @DisplayName("유효 시간 내의 인증 코드를 조회할 수 있다.")
    @Test
    void shouldFindAuthCode_whenWithinValidTime() {
        //given
        long now = System.currentTimeMillis();
        given(valueOperations.get(KEY_PREFIX + EMAIL)).willReturn(AUTH_CODE + ":" + now);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        //when
        Optional<String> result = redisEmailAuthCodeRepository.findByEmail(EMAIL);

        //then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(AUTH_CODE);
    }

    @DisplayName("인증 코드가 존재하지 않으면 예외를 던진다.")
    @Test
    void shouldThrowException_whenAuthCodeNotFound() {
        //given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY_PREFIX + EMAIL)).willReturn(null);

        //when, then
        assertThatThrownBy(() -> redisEmailAuthCodeRepository.findByEmail(EMAIL))
                .isInstanceOf(DentruthException.class)
                .satisfies(e -> {
                    DentruthException ex = (DentruthException) e;
                    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatus.INVALID_AUTH_CODE);
                });
    }

    @DisplayName("유효 시간(5분)이 지난 인증 코드는 예외를 던진다.")
    @Test
    void shouldThrowException_whenAuthCodeExpired() {
        //given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000);
        given(valueOperations.get(KEY_PREFIX + EMAIL)).willReturn(AUTH_CODE + ":" + sixMinutesAgo);

        //when, then
        assertThatThrownBy(() -> redisEmailAuthCodeRepository.findByEmail(EMAIL))
                .isInstanceOf(DentruthException.class)
                .satisfies(e -> {
                    DentruthException ex = (DentruthException) e;
                    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatus.EXPIRED_AUTH_CODE);
                });
    }

    @DisplayName("이메일 인증 코드를 삭제할 수 있다.")
    @Test
    void shouldDeleteAuthCode_successfully() {
        //when
        redisEmailAuthCodeRepository.deleteByEmail(EMAIL);

        //then
        verify(redisTemplate).delete(KEY_PREFIX + EMAIL);
    }

}
