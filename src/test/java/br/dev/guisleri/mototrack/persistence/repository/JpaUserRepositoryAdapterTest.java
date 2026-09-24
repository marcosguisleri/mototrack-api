package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaUserRepositoryAdapter.class, UserMapper.class})
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter userRepositoryAdapter;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistNewUser() {
        User newUser = User.register(
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );

        assertNull(newUser.getId());

        User savedUser = save(newUser);
        entityManager.clear();
        UserEntity savedEntity = springDataUserRepository
                .findById(savedUser.getId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(savedUser.getId()),
                () -> assertEquals(savedUser.getId(), savedEntity.getId()),
                () -> assertEquals("Marcos", savedUser.getName()),
                () -> assertEquals("marcos@example.com", savedUser.getEmail()),
                () -> assertEquals("password-hash", savedUser.getPasswordHash()),
                () -> assertEquals("Marcos", savedEntity.getName()),
                () -> assertEquals("marcos@example.com", savedEntity.getEmail()),
                () -> assertEquals("password-hash", savedEntity.getPasswordHash())
        );
    }

    @Test
    void shouldFindUserByEmail() {
        User savedUser = save(User.register(
                "Marcos",
                "marcos@example.com",
                "password-hash"
        ));
        entityManager.clear();

        User result = userRepositoryAdapter
                .findByEmail("marcos@example.com")
                .orElseThrow();

        assertAll(
                () -> assertEquals(savedUser.getId(), result.getId()),
                () -> assertEquals("Marcos", result.getName()),
                () -> assertEquals("marcos@example.com", result.getEmail()),
                () -> assertEquals("password-hash", result.getPasswordHash())
        );
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        assertTrue(userRepositoryAdapter
                .findByEmail("unknown@example.com")
                .isEmpty());
    }

    @Test
    void shouldRejectDuplicateEmailAtDatabaseLevel() {
        save(User.register(
                "Marcos",
                "duplicate@example.com",
                "password-hash"
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepositoryAdapter.save(
                        User.register(
                                "Outro usuário",
                                "duplicate@example.com",
                                "other-password-hash"
                        )
                )
        );
    }

    private User save(User user) {
        User savedUser = userRepositoryAdapter.save(user);
        springDataUserRepository.flush();
        return savedUser;
    }
}
