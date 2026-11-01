package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

    // -------------------------------------------------------------------------
    // Registro de novos usuários
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Restauração de usuários persistidos
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Troca de e-mail e de senha
    // -------------------------------------------------------------------------

    @Test
    void shouldChangeEmailNormalizingIt() {
        User user = User.restore(1L, "Marcos", "marcos@example.com", "hash");

        user.changeEmail("  Novo@Example.COM ");

        assertEquals("novo@example.com", user.getEmail());
    }

    @Test
    void shouldRefuseABlankEmail() {
        User user = User.restore(1L, "Marcos", "marcos@example.com", "hash");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> user.changeEmail("  ")
        );
        assertEquals("marcos@example.com", user.getEmail());
    }

    @Test
    void shouldChangePasswordHashAndRefuseABlankOne() {
        User user = User.restore(1L, "Marcos", "marcos@example.com", "hash");

        user.changePasswordHash("new-hash");
        assertEquals("new-hash", user.getPasswordHash());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> user.changePasswordHash(" ")
        );
        assertEquals("new-hash", user.getPasswordHash());
    }
}
