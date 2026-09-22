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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    @Test
    void shouldRegisterUserWithNormalizedEmail() {
        User persistedUser = User.restore(1L, "Marcos", "marcos@example.com");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        User result = service.registerUser(
                "Marcos",
                "  Marcos@Example.COM  "
        );

        verify(userRepository).findByEmail("marcos@example.com");
        verify(userRepository).save(userCaptor.capture());
        User userSentToRepository = userCaptor.getValue();
        assertNull(userSentToRepository.getId());
        assertEquals("Marcos", userSentToRepository.getName());
        assertEquals("marcos@example.com", userSentToRepository.getEmail());
        assertSame(persistedUser, result);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User existingUser = User.restore(1L, "Marcos", "marcos@example.com");
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.of(existingUser));

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> service.registerUser(
                        "Outro usuário",
                        "  Marcos@Example.COM  "
                )
        );

        assertEquals(
                "Já existe um usuário cadastrado com este e-mail.",
                exception.getMessage()
        );
        verify(userRepository).findByEmail("marcos@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFindUserById() {
        User user = User.restore(1L, "Marcos", "marcos@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = service.findUserById(1L);

        assertSame(user, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenFindingUnknownUserById() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> service.findUserById(999L)
        );

        assertEquals("Usuário com id 999 não encontrado", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    void shouldFindUserByNormalizedEmail() {
        User user = User.restore(1L, "Marcos", "marcos@example.com");
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

    @Test
    void shouldFindAllUsers() {
        List<User> users = List.of(
                User.restore(1L, "Marcos", "marcos@example.com"),
                User.restore(2L, "Ana", "ana@example.com")
        );
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = service.findAllUsers();

        assertSame(users, result);
        verify(userRepository).findAll();
    }
}
