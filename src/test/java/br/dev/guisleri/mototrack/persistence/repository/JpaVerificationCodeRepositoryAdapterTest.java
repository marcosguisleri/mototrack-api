package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.VerificationCodeMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaVerificationCodeRepositoryAdapter.class, VerificationCodeMapper.class})
class JpaVerificationCodeRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    @Autowired
    private JpaVerificationCodeRepositoryAdapter adapter;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = springDataUserRepository.saveAndFlush(
                new UserEntity(null, "Marcos", "marcos@example.com", "hash")
        );
    }

    private VerificationCode code(VerificationPurpose purpose, String newEmail) {
        return VerificationCode.create(
                user.getId(), purpose, "code-hash", newEmail, NOW, NOW.plusSeconds(900)
        );
    }

    @Test
    void shouldPersistAndFindTheCodeOfAUserAndPurpose() {
        VerificationCode saved = adapter.save(code(VerificationPurpose.EMAIL_CHANGE, "novo@example.com"));
        entityManager.flush();
        entityManager.clear();

        VerificationCode found = adapter
                .findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.EMAIL_CHANGE)
                .orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), found.getId());
        assertEquals("code-hash", found.getCodeHash());
        assertEquals("novo@example.com", found.getNewEmail());
        assertEquals(NOW, found.getCreatedAt());
        assertEquals(NOW.plusSeconds(900), found.getExpiresAt());
        assertEquals(0, found.getAttempts());
    }

    @Test
    void shouldKeepCodesOfDifferentPurposesApart() {
        adapter.save(code(VerificationPurpose.PASSWORD_RESET, null));
        adapter.save(code(VerificationPurpose.EMAIL_CHANGE, "novo@example.com"));
        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter.findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.PASSWORD_RESET).isPresent());
        assertEquals(
                "novo@example.com",
                adapter.findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.EMAIL_CHANGE)
                        .orElseThrow().getNewEmail()
        );
    }

    @Test
    void shouldPersistFailedAttempts() {
        VerificationCode saved = adapter.save(code(VerificationPurpose.PASSWORD_RESET, null));
        saved.registerFailedAttempt();
        saved.registerFailedAttempt();
        adapter.save(saved);
        entityManager.flush();
        entityManager.clear();

        assertEquals(
                2,
                adapter.findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.PASSWORD_RESET)
                        .orElseThrow().getAttempts()
        );
    }

    @Test
    void shouldDeleteByUserAndPurposeAndAllowANewCodeRightAfter() {
        adapter.save(code(VerificationPurpose.PASSWORD_RESET, null));
        entityManager.flush();

        adapter.deleteByUserIdAndPurpose(user.getId(), VerificationPurpose.PASSWORD_RESET);
        // The unique constraint (user, purpose) must not be hit by the replacement.
        adapter.save(code(VerificationPurpose.PASSWORD_RESET, null));
        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter.findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.PASSWORD_RESET).isPresent());
    }

    @Test
    void shouldDeleteById() {
        VerificationCode saved = adapter.save(code(VerificationPurpose.PASSWORD_RESET, null));
        entityManager.flush();

        adapter.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(adapter.findByUserIdAndPurposeForUpdate(user.getId(), VerificationPurpose.PASSWORD_RESET).isEmpty());
    }

    @Test
    void shouldRefuseACodeForAnUnknownUser() {
        VerificationCode orphan = VerificationCode.create(
                999L, VerificationPurpose.PASSWORD_RESET, "h", null, NOW, NOW.plusSeconds(900)
        );

        assertThrows(UserNotFoundException.class, () -> adapter.save(orphan));
    }
}
