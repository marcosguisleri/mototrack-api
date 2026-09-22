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

import java.util.List;

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
        User newUser = User.register("Marcos", "marcos@example.com");

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
                () -> assertEquals("Marcos", savedEntity.getName()),
                () -> assertEquals("marcos@example.com", savedEntity.getEmail())
        );
    }

    @Test
    void shouldFindUserById() {
        User savedUser = save(User.register("Marcos", "marcos@example.com"));
        entityManager.clear();

        User result = userRepositoryAdapter.findById(savedUser.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(savedUser.getId(), result.getId()),
                () -> assertEquals("Marcos", result.getName()),
                () -> assertEquals("marcos@example.com", result.getEmail())
        );
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        assertTrue(userRepositoryAdapter.findById(999L).isEmpty());
    }

    @Test
    void shouldFindUserByEmail() {
        User savedUser = save(User.register("Marcos", "marcos@example.com"));
        entityManager.clear();

        User result = userRepositoryAdapter
                .findByEmail("marcos@example.com")
                .orElseThrow();

        assertAll(
                () -> assertEquals(savedUser.getId(), result.getId()),
                () -> assertEquals("Marcos", result.getName()),
                () -> assertEquals("marcos@example.com", result.getEmail())
        );
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        assertTrue(userRepositoryAdapter
                .findByEmail("unknown@example.com")
                .isEmpty());
    }

    @Test
    void shouldFindAllUsers() {
        User marcos = save(User.register("Marcos", "marcos@example.com"));
        User ana = save(User.register("Ana", "ana@example.com"));
        entityManager.clear();

        List<User> result = userRepositoryAdapter.findAll();

        assertEquals(2, result.size());
        assertTrue(result.stream().map(User::getId).toList()
                .containsAll(List.of(marcos.getId(), ana.getId())));
    }

    @Test
    void shouldRejectDuplicateEmailAtDatabaseLevel() {
        save(User.register("Marcos", "duplicate@example.com"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepositoryAdapter.save(
                        User.register("Outro usuário", "duplicate@example.com")
                )
        );
    }

    private User save(User user) {
        User savedUser = userRepositoryAdapter.save(user);
        springDataUserRepository.flush();
        return savedUser;
    }
}
