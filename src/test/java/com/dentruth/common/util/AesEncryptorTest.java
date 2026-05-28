package com.dentruth.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AesEncryptorTest {

    private AesEncryptor aesEncryptor;

    @BeforeEach
    void setUp() {
        aesEncryptor = new AesEncryptor();

        // 🚨 끝에 'x' 하나만 더 붙여서 정확히 32글자(32바이트)로 맞추기!
        ReflectionTestUtils.setField(aesEncryptor, "secretKey", "dentruthSecretKey2026Project256x");

        // iv는 16글자가 맞음 (dentruthIvVector = 16자)
        ReflectionTestUtils.setField(aesEncryptor, "iv", "dentruthIvVector");
    }

    @DisplayName("정상 키 스펙이 주입되면 암복호화가 완벽하게 성공한다.")
    @Test
    void encryptDecrypt_roundtrip() {
        // given
        String plainText = "안녕하세요 덴트루스 테스트 문장입니다.";

        // when
        String encrypted = aesEncryptor.encrypt(plainText);
        String decrypted = aesEncryptor.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(plainText);
    }
}