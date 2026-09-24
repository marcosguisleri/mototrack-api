package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PLAIN_PASSWORD = "  secret123  ";
    private static final String PASSWORD_HASH = "$2a$10$encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterUserWithNormalizedEmailAndEncodedPassword() {
        User persistedUser = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                PASSWORD_HASH
        );
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        User result = service.registerUser(
                "Marcos",
                "  Marcos@Example.COM  ",
                PLAIN_PASSWORD
        );

        verify(userRepository).findByEmail("marcos@example.com");
        verify(passwordEncoder).encode(PLAIN_PASSWORD);
        verify(userRepository).save(userCaptor.capture());
        User userSentToRepository = userCaptor.getValue();
        assertNull(userSentToRepository.getId());
        assertEquals("Marcos", userSentToRepository.getName());
        assertEquals("marcos@example.com", userSentToRepository.getEmail());
        assertEquals(PASSWORD_HASH, userSentToRepository.getPasswordHash());
        assertNotEquals(PLAIN_PASSWORD, userSentToRepository.getPasswordHash());
        assertSame(persistedUser, result);
    }

    @Test
    void shouldRejectDuplicateNormalizedEmailBeforeEncodingPassword() {
        User existingUser = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                PASSWORD_HASH
        );
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.of(existingUser));

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> service.registerUser(
                        "Outro usuário",
                        "  Marcos@Example.COM  ",
                        PLAIN_PASSWORD
                )
        );

        assertEquals(
                "Já existe um usuário cadastrado com este e-mail.",
                exception.getMessage()
        );
        verify(userRepository).findByEmail("marcos@example.com");
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldConvertDuplicateEmailRaceConditionToUserAlreadyExists() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique email"));

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> service.registerUser(
                        "Marcos",
                        "  Marcos@Example.COM  ",
                        PLAIN_PASSWORD
                )
        );

        assertEquals(
                "Já existe um usuário cadastrado com este e-mail.",
                exception.getMessage()
        );
        verify(userRepository).findByEmail("marcos@example.com");
        verify(passwordEncoder).encode(PLAIN_PASSWORD);
        verify(userRepository).save(userCaptor.capture());
        User userSentToRepository = userCaptor.getValue();
        assertEquals("marcos@example.com", userSentToRepository.getEmail());
        assertEquals(PASSWORD_HASH, userSentToRepository.getPasswordHash());
        assertNotEquals(PLAIN_PASSWORD, userSentToRepository.getPasswordHash());
    }

    @Test
    void shouldFindUserByNormalizedEmail() {
        User user = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                PASSWORD_HASH
        );
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.of(user));

        User result = service.findUserByEmail("  Marcos@Example.COM  ");

        assertSame(user, result);
        verify(userRepository).findByEmail("marcos@example.com");
    }

    @Test
    void shouldThrowWhenFindingUnknownUserByEmail() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> service.findUserByEmail("  Unknown@Example.COM  ")
        );

        assertEquals(
                "Usuário com email: unknown@example.com não encontrado",
                exception.getMessage()
        );
        verify(userRepository).findByEmail("unknown@example.com");
    }
}
