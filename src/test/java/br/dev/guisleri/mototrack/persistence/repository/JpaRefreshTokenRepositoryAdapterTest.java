package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.persistence.entity.RefreshTokenEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.RefreshTokenMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaRefreshTokenRepositoryAdapter.class, RefreshTokenMapper.class})
class JpaRefreshTokenRepositoryAdapterTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-31T12:00:00Z");
    private static final String TOKEN_HASH = "refresh-token-hash-1";

    @Autowired
    private JpaRefreshTokenRepositoryAdapter refreshTokenRepositoryAdapter;

    @Autowired
    private SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        userEntity = springDataUserRepository.saveAndFlush(
                new UserEntity(null, "Marcos", "marcos@example.com", "password-hash")
        );
    }

    // -------------------------------------------------------------------------
    // Persistência e vínculo do refresh token com o usuário
    // -------------------------------------------------------------------------

    @Test
    void shouldPersistRefreshTokenWithUser() {
        RefreshToken newRefreshToken = RefreshToken.create(
                userEntity.getId(),
                TOKEN_HASH,
                EXPIRES_AT
        );

        RefreshToken savedRefreshToken = refreshTokenRepositoryAdapter.save(newRefreshToken);
        springDataRefreshTokenRepository.flush();
        entityManager.clear();

        RefreshTokenEntity savedEntity = springDataRefreshTokenRepository
                .findById(savedRefreshToken.getId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(savedRefreshToken.getId()),
                () -> assertEquals(savedRefreshToken.getId(), savedEntity.getId()),
                () -> assertEquals(userEntity.getId(), savedRefreshToken.getUserId()),
                () -> assertEquals(userEntity.getId(), savedEntity.getUser().getId()),
                () -> assertEquals(TOKEN_HASH, savedRefreshToken.getTokenHash()),
                () -> assertEquals(TOKEN_HASH, savedEntity.getTokenHash()),
                () -> assertEquals(EXPIRES_AT, savedRefreshToken.getExpiresAt()),
                () -> assertEquals(EXPIRES_AT, savedEntity.getExpiresAt())
        );
    }

    @Test
    void shouldThrowWhenSavingRefreshTokenForUnknownUser() {
        RefreshToken refreshToken = RefreshToken.create(
                999L,
                TOKEN_HASH,
                EXPIRES_AT
        );

        assertThrows(
                UserNotFoundException.class,
                () -> refreshTokenRepositoryAdapter.save(refreshToken)
        );

        assertEquals(0, springDataRefreshTokenRepository.count());
    }

    // -------------------------------------------------------------------------
    // Consulta por hash com lock para atualização
    // -------------------------------------------------------------------------

    @Test
    void shouldFindRefreshTokenByTokenHashForUpdate() {
        RefreshToken savedRefreshToken = saveRefreshToken(TOKEN_HASH, EXPIRES_AT);
        entityManager.clear();

        RefreshToken result = refreshTokenRepositoryAdapter
                .findByTokenHashForUpdate(TOKEN_HASH)
                .orElseThrow();
        RefreshTokenEntity lockedEntity = entityManager.find(
                RefreshTokenEntity.class,
                savedRefreshToken.getId()
        );

        assertAll(
                () -> assertEquals(savedRefreshToken.getId(), result.getId()),
                () -> assertEquals(userEntity.getId(), result.getUserId()),
                () -> assertEquals(TOKEN_HASH, result.getTokenHash()),
                () -> assertEquals(EXPIRES_AT, result.getExpiresAt()),
                () -> assertEquals(
                        LockModeType.PESSIMISTIC_WRITE,
                        entityManager.getLockMode(lockedEntity)
                )
        );
    }

    @Test
    void shouldReturnEmptyWhenTokenHashDoesNotExist() {
        assertTrue(refreshTokenRepositoryAdapter
                .findByTokenHashForUpdate("unknown-token-hash")
                .isEmpty());
    }

    // -------------------------------------------------------------------------
    // Exclusão por ID
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteRefreshTokenById() {
        RefreshToken savedRefreshToken = saveRefreshToken(TOKEN_HASH, EXPIRES_AT);

        refreshTokenRepositoryAdapter.deleteById(savedRefreshToken.getId());
        springDataRefreshTokenRepository.flush();

        assertTrue(springDataRefreshTokenRepository
                .findById(savedRefreshToken.getId())
                .isEmpty());
    }

    // -------------------------------------------------------------------------
    // Cardinalidade de tokens por usuário
    // -------------------------------------------------------------------------

    @Test
    void shouldPersistMultipleRefreshTokensForSameUser() {
        Instant secondExpiresAt = Instant.parse("2026-11-30T12:00:00Z");

        RefreshToken firstToken = saveRefreshToken(TOKEN_HASH, EXPIRES_AT);
        RefreshToken secondToken = saveRefreshToken(
                "refresh-token-hash-2",
                secondExpiresAt
        );
        entityManager.clear();

        RefreshToken firstResult = refreshTokenRepositoryAdapter
                .findByTokenHashForUpdate(TOKEN_HASH)
                .orElseThrow();
        RefreshToken secondResult = refreshTokenRepositoryAdapter
                .findByTokenHashForUpdate("refresh-token-hash-2")
                .orElseThrow();

        assertAll(
                () -> assertEquals(2, springDataRefreshTokenRepository.count()),
                () -> assertEquals(firstToken.getId(), firstResult.getId()),
                () -> assertEquals(userEntity.getId(), firstResult.getUserId()),
                () -> assertEquals(TOKEN_HASH, firstResult.getTokenHash()),
                () -> assertEquals(EXPIRES_AT, firstResult.getExpiresAt()),
                () -> assertEquals(secondToken.getId(), secondResult.getId()),
                () -> assertEquals(userEntity.getId(), secondResult.getUserId()),
                () -> assertEquals("refresh-token-hash-2", secondResult.getTokenHash()),
                () -> assertEquals(secondExpiresAt, secondResult.getExpiresAt())
        );
    }

    // -------------------------------------------------------------------------
    // Helpers de persistência
    // -------------------------------------------------------------------------

    private RefreshToken saveRefreshToken(String tokenHash, Instant expiresAt) {
        RefreshToken savedRefreshToken = refreshTokenRepositoryAdapter.save(
                RefreshToken.create(userEntity.getId(), tokenHash, expiresAt)
        );
        springDataRefreshTokenRepository.flush();
        return savedRefreshToken;
    }

    // -------------------------------------------------------------------------
    // Encerrar todas as sessões de um usuário
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteEveryRefreshTokenOfTheUserAndOnlyHis() {
        UserEntity other = springDataUserRepository.saveAndFlush(
                new UserEntity(null, "Outro", "outro@example.com", "password-hash")
        );

        refreshTokenRepositoryAdapter.save(RefreshToken.create(userEntity.getId(), "hash-a", EXPIRES_AT));
        refreshTokenRepositoryAdapter.save(RefreshToken.create(userEntity.getId(), "hash-b", EXPIRES_AT));
        refreshTokenRepositoryAdapter.save(RefreshToken.create(other.getId(), "hash-c", EXPIRES_AT));
        springDataRefreshTokenRepository.flush();

        refreshTokenRepositoryAdapter.deleteAllByUserId(userEntity.getId());
        entityManager.clear();

        assertTrue(refreshTokenRepositoryAdapter.findByTokenHashForUpdate("hash-a").isEmpty());
        assertTrue(refreshTokenRepositoryAdapter.findByTokenHashForUpdate("hash-b").isEmpty());
        assertTrue(refreshTokenRepositoryAdapter.findByTokenHashForUpdate("hash-c").isPresent());
    }
}
