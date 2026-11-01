package br.dev.guisleri.mototrack.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita o tamanho de um texto em bytes UTF-8. O BCrypt só aceita senhas de até 72 bytes
 * (e recusa as maiores), e acentos e emojis ocupam mais de um byte por caractere.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "O texto é longo demais.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
