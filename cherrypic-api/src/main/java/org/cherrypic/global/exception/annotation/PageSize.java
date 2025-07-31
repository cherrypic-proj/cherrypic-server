package org.cherrypic.global.exception.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.cherrypic.global.exception.validator.PageSizeValidator;

@Constraint(validatedBy = PageSizeValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PageSize {
    String message() default "페이지 크기는 0보다 커야합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
