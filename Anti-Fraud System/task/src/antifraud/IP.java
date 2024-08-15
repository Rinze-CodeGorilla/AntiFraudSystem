package antifraud;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

@Entity
public class IP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    public String ip;

    public IP(String ip) {
        this.ip = ip;
    }

    public IP() {
    }

    public static boolean verify(String ip) {
        var parts = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
        if (parts.length != 4) return false;
        return !Arrays.stream(parts).anyMatch(b -> b < 0 || b > 255);
    }
}

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {IPValidator.class})
@interface IPConstraint {
    String message() default "{antifraud.IP.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class IPValidator implements ConstraintValidator<IPConstraint, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return IP.verify(value);
    }
}