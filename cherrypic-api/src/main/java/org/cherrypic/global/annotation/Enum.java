package org.cherrypic.global.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.cherrypic.global.validator.EnumValidator;

/** RequestBody 의 Enum 검증을 위한 어노테이션 입니다 */
@Constraint(validatedBy = {EnumValidator.class})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Enum {
    String message() default "적절하지 않은 Enum값 입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
