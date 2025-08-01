package org.cherrypic.global.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.cherrypic.global.validator.PageSizeValidator;

/** Query Parameter 의 PageSize 검증을 위한 어노테이션 입니다 */
@Constraint(validatedBy = PageSizeValidator.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PageSize {
    String message() default "페이지 크기는 0보다 큰 값만 가능합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
