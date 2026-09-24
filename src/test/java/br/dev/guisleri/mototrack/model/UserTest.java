package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

    @Test
    void shouldRegisterNewUserWithoutIdAndWithPasswordHash() {
        User user = User.register(
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );

        assertAll(
                () -> assertNull(user.getId()),
                () -> assertEquals("Marcos", user.getName()),
                () -> assertEquals("marcos@example.com", user.getEmail()),
                () -> assertEquals("password-hash", user.getPasswordHash())
        );
    }

    @Test
    void shouldRestorePersistedUser() {
        User user = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );

        assertAll(
                () -> assertEquals(1L, user.getId()),
                () -> assertEquals("Marcos", user.getName()),
                () -> assertEquals("marcos@example.com", user.getEmail()),
                () -> assertEquals("password-hash", user.getPasswordHash())
        );
    }
}
