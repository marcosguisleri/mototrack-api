package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.model.VerificationPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeGeneratorTest {

    private static final String SECRET = "mototrack-test-only-hs256-secret-key-32-bytes-minimum";

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator(SECRET);

    @Test
    void shouldGenerateSixDigitCodesKeepingLeadingZeros() {
        for (int i = 0; i < 500; i++) {
            assertTrue(generator.generate().matches("\\d{6}"));
        }
    }

    @Test
    void shouldGenerateDifferentCodes() {
        assertNotEquals(generator.generate(), generator.generate());
    }

    @Test
    void shouldHashDeterministicallyAndNeverStoreTheCode() {
        String hash = generator.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456");

        assertEquals(hash, generator.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456"));
        assertFalse(hash.contains("123456"));
    }

    @Test
    void shouldTieTheHashToTheUserAndThePurpose() {
        String hash = generator.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456");

        assertNotEquals(hash, generator.hash(2L, VerificationPurpose.PASSWORD_RESET, "123456"));
        assertNotEquals(hash, generator.hash(1L, VerificationPurpose.EMAIL_CHANGE, "123456"));
    }

    @Test
    void shouldTieTheHashToTheServerKey() {
        VerificationCodeGenerator other =
                new VerificationCodeGenerator("another-secret-key-with-at-least-32-bytes!!");

        assertNotEquals(
                generator.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456"),
                other.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456")
        );
    }

    @Test
    void shouldMatchOnlyTheRightCode() {
        String hash = generator.hash(1L, VerificationPurpose.PASSWORD_RESET, "123456");

        assertTrue(generator.matches(hash, 1L, VerificationPurpose.PASSWORD_RESET, "123456"));
        assertFalse(generator.matches(hash, 1L, VerificationPurpose.PASSWORD_RESET, "654321"));
        assertFalse(generator.matches(hash, 2L, VerificationPurpose.PASSWORD_RESET, "123456"));
    }
}
