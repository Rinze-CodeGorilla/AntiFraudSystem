package antifraud;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {LuhnValidator.class})
public @interface Luhn {
    String message() default "{Luhn.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
