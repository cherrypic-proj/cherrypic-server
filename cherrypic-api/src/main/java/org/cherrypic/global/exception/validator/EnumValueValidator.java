package org.cherrypic.global.exception.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.cherrypic.global.exception.annotation.EnumValue;

public class EnumValueValidator implements ConstraintValidator<EnumValue, Enum<?>> {

    private Class<? extends Enum<?>> enumClass;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(Enum<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            String enumName = enumClass.getSimpleName();
            String enumValues = getEnumValues();
            context.buildConstraintViolationWithTemplate(
                            enumName + " Enum의 값들 내에서 입력해야 합니다. 가능한 값: " + enumValues)
                    .addConstraintViolation();
            return false;
        }

        boolean isValidEnum = false;
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant == value) {
                isValidEnum = true;
                break;
            }
        }

        if (!isValidEnum) {
            String enumName = enumClass.getSimpleName();
            String enumValues = getEnumValues();
            context.buildConstraintViolationWithTemplate(
                            enumName + " Enum의 값들 내에서 입력해야 합니다. 가능한 값: " + enumValues)
                    .addConstraintViolation();
        }

        return true;
    }

    private String getEnumValues() {
        StringBuilder enumValues = new StringBuilder();
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            enumValues.append(enumConstant.name()).append(", ");
        }
        if (enumValues.length() > 0) {
            enumValues.setLength(enumValues.length() - 2);
        }
        return enumValues.toString();
    }
}
