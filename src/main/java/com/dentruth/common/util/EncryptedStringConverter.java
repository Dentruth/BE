package com.dentruth.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    @Override
    public String convertToDatabaseColumn(String s) {
        return aesEncryptor.encrypt(s);
    }

    @Override
    public String convertToEntityAttribute(String s) {
        return aesEncryptor.decrypt(s);
    }

}
