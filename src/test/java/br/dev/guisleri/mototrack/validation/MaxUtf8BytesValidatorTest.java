package br.dev.guisleri.mototrack.validation;

import br.dev.guisleri.mototrack.dto.CreateUserRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxUtf8BytesValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean passwordIsRejected(String password) {
        return validator.validate(new CreateUserRequestDTO("Marcos", "marcos@example.com", password))
                .stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void shouldAcceptAPasswordOfExactly72Bytes() {
        assertThat(passwordIsRejected("a".repeat(72))).isFalse();
    }

    @Test
    void shouldRejectAPasswordOf73Bytes() {
        assertThat(passwordIsRejected("a".repeat(73))).isTrue();
    }

    @Test
    void shouldCountAccentedCharactersAsTwoBytes() {
        // 36 × "é" = 72 bytes; 37 × "é" = 74 bytes.
        assertThat(passwordIsRejected("é".repeat(36))).isFalse();
        assertThat(passwordIsRejected("é".repeat(37))).isTrue();
    }

    @Test
    void shouldCountEmojisAsFourBytes() {
        assertThat(passwordIsRejected("🏍".repeat(18))).isFalse();
        assertThat(passwordIsRejected("🏍".repeat(19))).isTrue();
    }
}
