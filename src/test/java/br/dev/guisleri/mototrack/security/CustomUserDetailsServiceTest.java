package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final User USER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com",
            "password-hash"
    );

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void shouldLoadUserByNormalizedEmailWithPasswordHashAndNoAuthorities() {
        when(userRepository.findByEmail("marcos@example.com"))
                .thenReturn(Optional.of(USER));

        UserDetails userDetails = userDetailsService.loadUserByUsername(
                "  Marcos@Example.COM  "
        );

        assertEquals("marcos@example.com", userDetails.getUsername());
        assertEquals("password-hash", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());
        verify(userRepository).findByEmail("marcos@example.com");
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "  Unknown@Example.COM  "
                )
        );

        verify(userRepository).findByEmail("unknown@example.com");
    }
}
