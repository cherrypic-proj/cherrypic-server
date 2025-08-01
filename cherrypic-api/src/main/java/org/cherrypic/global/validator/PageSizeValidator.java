package org.cherrypic.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.cherrypic.global.annotation.PageSize;

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
