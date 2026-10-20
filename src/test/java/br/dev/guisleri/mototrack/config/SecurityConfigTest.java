package br.dev.guisleri.mototrack.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    // -------------------------------------------------------------------------
    // Validação das propriedades de segurança JWT
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectJwtSecretShorterThan256Bits() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> securityConfig.jwtEncoder("a".repeat(31))
        );

        assertEquals(
                "security.jwt.secret must contain at least 32 UTF-8 bytes.",
                exception.getMessage()
        );
    }
}
