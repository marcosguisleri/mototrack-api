package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.exception.InvalidCurrentPasswordException;
import br.dev.guisleri.mototrack.exception.InvalidEmailChangeException;
import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.VerificationCodeCooldownException;
import br.dev.guisleri.mototrack.mail.EmailSender;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailSender emailSender;

    private EmailChangeService service;

    @BeforeEach
    void setUp() {
        service = new EmailChangeService(
                userRepository,
                verificationCodeService,
                authService,
                passwordEncoder,
                emailSender
        );
    }

    private User user() {
        return User.restore(1L, "Marcos", "marcos@example.com", "hash");
    }

    // -------------------------------------------------------------------------
    // Pedido de troca
    // -------------------------------------------------------------------------

    @Test
    void shouldSendTheCodeToTheNewAddressAfterCheckingThePassword() {
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(verificationCodeService.wasIssuedRecently(1L, VerificationPurpose.EMAIL_CHANGE))
                .thenReturn(false);
        when(verificationCodeService.issue(1L, VerificationPurpose.EMAIL_CHANGE, "novo@example.com"))
                .thenReturn("654321");

        service.requestChange(user(), " Novo@Example.com ", "secret123");

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("novo@example.com"), anyString(), text.capture());
        assertTrue(text.getValue().contains("654321"));
    }

    @Test
    void shouldRefuseAWrongCurrentPasswordBeforeAnythingElse() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        InvalidCurrentPasswordException exception = assertThrows(
                InvalidCurrentPasswordException.class,
                () -> service.requestChange(user(), "novo@example.com", "wrong")
        );

        assertEquals("Senha atual incorreta.", exception.getMessage());
        verifyNoInteractions(verificationCodeService);
        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldRefuseTheSameAddress() {
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);

        assertThrows(
                InvalidEmailChangeException.class,
                () -> service.requestChange(user(), "MARCOS@example.com", "secret123")
        );

        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldRefuseAnAddressThatBelongsToAnotherAccount() {
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(userRepository.findByEmail("outro@example.com")).thenReturn(Optional.of(
                User.restore(2L, "Outro", "outro@example.com", "x")
        ));

        assertThrows(
                UserAlreadyExistsException.class,
                () -> service.requestChange(user(), "outro@example.com", "secret123")
        );

        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldMakeTheRiderWaitBeforeAskingForAnotherCode() {
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(verificationCodeService.wasIssuedRecently(1L, VerificationPurpose.EMAIL_CHANGE))
                .thenReturn(true);

        assertThrows(
                VerificationCodeCooldownException.class,
                () -> service.requestChange(user(), "novo@example.com", "secret123")
        );

        verify(verificationCodeService, never()).issue(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Confirmação
    // -------------------------------------------------------------------------

    private VerificationCode emailCode() {
        return VerificationCode.restore(
                5L, 1L, VerificationPurpose.EMAIL_CHANGE, "h",
                "novo@example.com", NOW, NOW.plusSeconds(900), 0
        );
    }

    @Test
    void shouldChangeTheEmailOpenANewSessionAndWarnTheOldAddress() {
        User user = user();
        AuthTokenResponseDTO tokens = new AuthTokenResponseDTO("access", "refresh");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(verificationCodeService.consume(1L, VerificationPurpose.EMAIL_CHANGE, "654321"))
                .thenReturn(emailCode());
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);
        when(authService.issueNewSession(user)).thenReturn(tokens);

        AuthTokenResponseDTO result = service.confirmChange(1L, "654321");

        assertEquals(tokens, result);
        assertEquals("novo@example.com", user.getEmail());
        verify(emailSender).send(eq("marcos@example.com"), anyString(), anyString());
    }

    @Test
    void shouldKeepTheEmailWhenTheCodeIsWrong() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(verificationCodeService.consume(1L, VerificationPurpose.EMAIL_CHANGE, "000000"))
                .thenThrow(new InvalidVerificationCodeException("Código inválido ou expirado."));

        assertThrows(
                InvalidVerificationCodeException.class,
                () -> service.confirmChange(1L, "000000")
        );

        assertEquals("marcos@example.com", user.getEmail());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(authService);
        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldRefuseWhenSomeoneTookTheAddressMeanwhile() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(verificationCodeService.consume(1L, VerificationPurpose.EMAIL_CHANGE, "654321"))
                .thenReturn(emailCode());
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.of(
                User.restore(2L, "Outro", "novo@example.com", "x")
        ));

        assertThrows(
                UserAlreadyExistsException.class,
                () -> service.confirmChange(1L, "654321")
        );

        assertEquals("marcos@example.com", user.getEmail());
        verify(userRepository, never()).save(any());
    }
}
