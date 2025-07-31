package org.cherrypic.global.exception.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.cherrypic.global.exception.annotation.PageSize;

public class PageSizeValidator implements ConstraintValidator<PageSize, Integer> {

    @Override
    public void initialize(PageSize constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value != null && value > 0;
    }
}
