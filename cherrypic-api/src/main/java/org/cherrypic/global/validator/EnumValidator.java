package org.cherrypic.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import org.cherrypic.global.annotation.Enum;

public class EnumValidator implements ConstraintValidator<Enum, java.lang.Enum> {
    @Override
    public boolean isValid(java.lang.Enum value, ConstraintValidatorContext context) {
        if (value == null) return false;
        Class<?> reflectionEnumClass = value.getDeclaringClass();
        return Arrays.asList(reflectionEnumClass.getEnumConstants()).contains(value);
    }
}
