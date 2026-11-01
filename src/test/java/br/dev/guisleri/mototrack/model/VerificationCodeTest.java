package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    private VerificationCode code() {
        return VerificationCode.create(
                1L,
                VerificationPurpose.PASSWORD_RESET,
                "hash",
                null,
                NOW,
                NOW.plusSeconds(900)
        );
    }

    @Test
    void shouldStartWithoutIdAndWithoutAttempts() {
        VerificationCode code = code();

        assertNull(code.getId());
        assertEquals(0, code.getAttempts());
        assertTrue(code.hasAttemptsLeft());
    }

    @Test
    void shouldExpireExactlyAtTheExpirationInstant() {
        VerificationCode code = code();

        assertFalse(code.isExpired(NOW.plusSeconds(899)));
        assertTrue(code.isExpired(NOW.plusSeconds(900)));
        assertTrue(code.isExpired(NOW.plusSeconds(901)));
    }

    @Test
    void shouldRunOutOfAttemptsAfterTheMaximum() {
        VerificationCode code = code();

        for (int i = 0; i < VerificationCode.MAX_ATTEMPTS - 1; i++) {
            code.registerFailedAttempt();
        }
        assertTrue(code.hasAttemptsLeft());

        code.registerFailedAttempt();
        assertFalse(code.hasAttemptsLeft());
    }

    @Test
    void shouldTellWhetherItWasCreatedAfterAnInstant() {
        VerificationCode code = code();

        assertTrue(code.wasCreatedAfter(NOW.minusSeconds(1)));
        assertFalse(code.wasCreatedAfter(NOW));
    }

    @Test
    void shouldRestorePersistedCodeKeepingAttemptsAndNewEmail() {
        VerificationCode code = VerificationCode.restore(
                7L, 1L, VerificationPurpose.EMAIL_CHANGE, "hash",
                "novo@example.com", NOW, NOW.plusSeconds(900), 3
        );

        assertEquals(7L, code.getId());
        assertEquals("novo@example.com", code.getNewEmail());
        assertEquals(3, code.getAttempts());
    }
}
