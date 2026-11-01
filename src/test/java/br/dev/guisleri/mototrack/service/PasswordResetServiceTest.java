package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.mail.EmailSender;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.RefreshTokenRepository;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailSender emailSender;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                refreshTokenRepository,
                verificationCodeService,
                passwordEncoder,
                emailSender
        );
    }

    private User user() {
        return User.restore(1L, "Marcos", "marcos@example.com", "old-hash");
    }

    // -------------------------------------------------------------------------
    // Pedido do código
    // -------------------------------------------------------------------------

    @Test
    void shouldEmailTheCodeToAnExistingAccountNormalizingTheAddress() {
        when(userRepository.findByEmail("marcos@example.com")).thenReturn(Optional.of(user()));
        when(verificationCodeService.wasIssuedRecently(1L, VerificationPurpose.PASSWORD_RESET))
                .thenReturn(false);
        when(verificationCodeService.issue(1L, VerificationPurpose.PASSWORD_RESET, null))
                .thenReturn("123456");

        service.requestReset("  Marcos@Example.COM ");

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(
                org.mockito.ArgumentMatchers.eq("marcos@example.com"),
                anyString(),
                text.capture()
        );
        assertTrue(text.getValue().contains("123456"));
        assertTrue(text.getValue().contains("Marcos"));
        assertTrue(text.getValue().contains("15 minutos"));
    }

    @Test
    void shouldDoNothingForAnUnknownAddressWithoutFailing() {
        when(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

        service.requestReset("ninguem@example.com");

        verifyNoInteractions(verificationCodeService);
        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldIgnoreARepeatedRequestWithinTheCooldown() {
        when(userRepository.findByEmail("marcos@example.com")).thenReturn(Optional.of(user()));
        when(verificationCodeService.wasIssuedRecently(1L, VerificationPurpose.PASSWORD_RESET))
                .thenReturn(true);

        service.requestReset("marcos@example.com");

        verify(verificationCodeService, never()).issue(any(), any(), any());
        verifyNoInteractions(emailSender);
    }

    // -------------------------------------------------------------------------
    // Troca da senha
    // -------------------------------------------------------------------------

    @Test
    void shouldChangeThePasswordAndCloseEverySession() {
        User user = user();
        when(userRepository.findByEmail("marcos@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret-1")).thenReturn("new-hash");

        service.resetPassword("Marcos@Example.com", "123456", "new-secret-1");

        verify(verificationCodeService).consume(1L, VerificationPurpose.PASSWORD_RESET, "123456");
        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    void shouldNotChangeAnythingWhenTheCodeIsWrong() {
        User user = user();
        when(userRepository.findByEmail("marcos@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeService.consume(1L, VerificationPurpose.PASSWORD_RESET, "000000"))
                .thenThrow(new InvalidVerificationCodeException("Código inválido ou expirado."));

        assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.resetPassword("marcos@example.com", "000000", "new-secret-1")
        );

        assertEquals("old-hash", user.getPasswordHash());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void shouldAnswerLikeAWrongCodeForAnUnknownAccount() {
        when(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

        InvalidVerificationCodeException exception = assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.resetPassword("ninguem@example.com", "123456", "new-secret-1")
        );

        assertEquals("Código inválido ou expirado.", exception.getMessage());
        assertFalse(exception.getMessage().contains("ninguem"));
        verifyNoInteractions(refreshTokenRepository);
    }
}
