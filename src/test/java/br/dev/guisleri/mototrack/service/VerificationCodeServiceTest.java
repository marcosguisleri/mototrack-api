package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.VerificationCodeRepository;
import br.dev.guisleri.mototrack.security.VerificationCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static final Long USER_ID = 1L;
    private static final VerificationPurpose PURPOSE = VerificationPurpose.PASSWORD_RESET;

    @Mock
    private VerificationCodeRepository repository;

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator(
            "mototrack-test-only-hs256-secret-key-32-bytes-minimum"
    );

    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        service = new VerificationCodeService(
                repository,
                generator,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private VerificationCode stored(String code, Instant expiresAt, int attempts) {
        return VerificationCode.restore(
                10L, USER_ID, PURPOSE, generator.hash(USER_ID, PURPOSE, code),
                null, NOW.minusSeconds(120), expiresAt, attempts
        );
    }

    // -------------------------------------------------------------------------
    // Emissão
    // -------------------------------------------------------------------------

    @Test
    void shouldIssueASixDigitCodeStoringOnlyItsHashAndReplacingTheOldOne() {
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        String code = service.issue(USER_ID, PURPOSE, null);

        verify(repository).deleteByUserIdAndPurpose(USER_ID, PURPOSE);
        verify(repository).save(captor.capture());

        VerificationCode saved = captor.getValue();
        assertTrue(code.matches("\\d{6}"));
        assertNotEquals(code, saved.getCodeHash());
        assertTrue(generator.matches(saved.getCodeHash(), USER_ID, PURPOSE, code));
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals(NOW.plus(VerificationCodeService.VALIDITY), saved.getExpiresAt());
        assertEquals(0, saved.getAttempts());
    }

    @Test
    void shouldKeepTheNewEmailOfAnEmailChangeCode() {
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.issue(USER_ID, VerificationPurpose.EMAIL_CHANGE, "novo@example.com");

        verify(repository).save(captor.capture());
        assertEquals("novo@example.com", captor.getValue().getNewEmail());
    }

    @Test
    void shouldKnowWhenACodeWasIssuedWithinTheLastMinute() {
        VerificationCode recent = VerificationCode.restore(
                10L, USER_ID, PURPOSE, "hash", null,
                NOW.minusSeconds(30), NOW.plusSeconds(870), 0
        );
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(recent));

        assertTrue(service.wasIssuedRecently(USER_ID, PURPOSE));
    }

    @Test
    void shouldNotConsiderOldOrMissingCodesAsRecent() {
        VerificationCode old = VerificationCode.restore(
                10L, USER_ID, PURPOSE, "hash", null,
                NOW.minusSeconds(61), NOW.plusSeconds(839), 0
        );
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(old), Optional.empty());

        assertFalse(service.wasIssuedRecently(USER_ID, PURPOSE));
        assertFalse(service.wasIssuedRecently(USER_ID, PURPOSE));
    }

    // -------------------------------------------------------------------------
    // Conferência
    // -------------------------------------------------------------------------

    @Test
    void shouldConsumeTheRightCode() {
        VerificationCode code = stored("123456", NOW.plusSeconds(600), 0);
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(code));

        VerificationCode consumed = service.consume(USER_ID, PURPOSE, "123456");

        assertEquals(10L, consumed.getId());
        verify(repository).deleteById(10L);
    }

    @Test
    void shouldCountAWrongCodeAsAnAttempt() {
        VerificationCode code = stored("123456", NOW.plusSeconds(600), 0);
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(code));

        InvalidVerificationCodeException exception = assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.consume(USER_ID, PURPOSE, "000000")
        );

        assertEquals("Código inválido ou expirado.", exception.getMessage());
        assertEquals(1, code.getAttempts());
        verify(repository).save(code);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void shouldKillTheCodeOnTheLastWrongAttempt() {
        VerificationCode code = stored("123456", NOW.plusSeconds(600), VerificationCode.MAX_ATTEMPTS - 1);
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(code));

        assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.consume(USER_ID, PURPOSE, "000000")
        );

        verify(repository).deleteById(10L);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRefuseTheRightCodeOnceAttemptsAreGone() {
        VerificationCode code = stored("123456", NOW.plusSeconds(600), VerificationCode.MAX_ATTEMPTS);
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(code));

        assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.consume(USER_ID, PURPOSE, "123456")
        );

        verify(repository).deleteById(10L);
    }

    @Test
    void shouldRefuseAnExpiredCodeEvenIfRight() {
        VerificationCode code = stored("123456", NOW.minusSeconds(1), 0);
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.of(code));

        assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.consume(USER_ID, PURPOSE, "123456")
        );

        verify(repository).deleteById(10L);
    }

    @Test
    void shouldGiveTheSameAnswerWhenThereIsNoCode() {
        when(repository.findByUserIdAndPurposeForUpdate(USER_ID, PURPOSE))
                .thenReturn(Optional.empty());

        InvalidVerificationCodeException exception = assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.consume(USER_ID, PURPOSE, "123456")
        );

        assertEquals("Código inválido ou expirado.", exception.getMessage());
    }
}
