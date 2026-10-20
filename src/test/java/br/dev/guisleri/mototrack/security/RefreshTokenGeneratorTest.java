package br.dev.guisleri.mototrack.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator refreshTokenGenerator =
            new RefreshTokenGenerator();

    // -------------------------------------------------------------------------
    // Geração de refresh tokens aleatórios e seguros para URL
    // -------------------------------------------------------------------------

    @Test
    void shouldGenerateNonBlankRefreshToken() {
        String refreshToken = refreshTokenGenerator.generate();

        assertAll(
                () -> assertNotNull(refreshToken),
                () -> assertFalse(refreshToken.isBlank())
        );
    }

    @Test
    void shouldGenerateDifferentRefreshTokens() {
        String firstToken = refreshTokenGenerator.generate();
        String secondToken = refreshTokenGenerator.generate();

        assertNotEquals(firstToken, secondToken);
    }

    @Test
    void shouldGenerateUrlSafeTokenWithoutPadding() {
        String refreshToken = refreshTokenGenerator.generate();

        assertAll(
                () -> assertEquals(43, refreshToken.length()),
                () -> assertTrue(refreshToken.matches("[A-Za-z0-9_-]+")),
                () -> assertFalse(refreshToken.contains("=")),
                () -> assertFalse(refreshToken.contains("+")),
                () -> assertFalse(refreshToken.contains("/"))
        );
    }

    // -------------------------------------------------------------------------
    // Hash SHA-256 dos refresh tokens
    // -------------------------------------------------------------------------

    @Test
    void shouldGenerateSameHashForSameToken() {
        String rawToken = "fixed-raw-refresh-token";

        String firstHash = refreshTokenGenerator.hash(rawToken);
        String secondHash = refreshTokenGenerator.hash(rawToken);

        assertEquals(firstHash, secondHash);
    }

    @Test
    void shouldGenerateDifferentHashesForDifferentTokens() {
        String firstHash = refreshTokenGenerator.hash("first-raw-refresh-token");
        String secondHash = refreshTokenGenerator.hash("second-raw-refresh-token");

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void shouldGenerateExpectedSha256Hash() {
        String result = refreshTokenGenerator.hash("test");

        assertEquals(
                "n4bQgYhMfWWaL-qgxVrQFaO_TxsrC4Is0V1sFbDwCgg",
                result
        );
    }
}
