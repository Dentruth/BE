package com.dentruth.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private List<String> enumValues;

    @Override
    public void initialize(ValidEnum annotation) {
        enumValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .toList();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // null은 @NotNull이 처리
        return enumValues.contains(value.toUpperCase());
    }

}
