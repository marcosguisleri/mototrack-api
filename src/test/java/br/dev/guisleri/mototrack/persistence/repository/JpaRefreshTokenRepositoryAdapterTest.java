package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.persistence.entity.RefreshTokenEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.RefreshTokenMapper;
import jakarta.persistence.EntityManager;
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

    @Test
    void shouldFindRefreshTokenByTokenHash() {
        RefreshToken savedRefreshToken = saveRefreshToken(TOKEN_HASH, EXPIRES_AT);
        entityManager.clear();

        RefreshToken result = refreshTokenRepositoryAdapter
                .findByTokenHash(TOKEN_HASH)
                .orElseThrow();

        assertAll(
                () -> assertEquals(savedRefreshToken.getId(), result.getId()),
                () -> assertEquals(userEntity.getId(), result.getUserId()),
                () -> assertEquals(TOKEN_HASH, result.getTokenHash()),
                () -> assertEquals(EXPIRES_AT, result.getExpiresAt())
        );
    }

    @Test
    void shouldReturnEmptyWhenTokenHashDoesNotExist() {
        assertTrue(refreshTokenRepositoryAdapter
                .findByTokenHash("unknown-token-hash")
                .isEmpty());
    }

    @Test
    void shouldDeleteRefreshTokenById() {
        RefreshToken savedRefreshToken = saveRefreshToken(TOKEN_HASH, EXPIRES_AT);

        refreshTokenRepositoryAdapter.deleteById(savedRefreshToken.getId());
        springDataRefreshTokenRepository.flush();

        assertTrue(springDataRefreshTokenRepository
                .findById(savedRefreshToken.getId())
                .isEmpty());
    }

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
                .findByTokenHash(TOKEN_HASH)
                .orElseThrow();
        RefreshToken secondResult = refreshTokenRepositoryAdapter
                .findByTokenHash("refresh-token-hash-2")
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

    private RefreshToken saveRefreshToken(String tokenHash, Instant expiresAt) {
        RefreshToken savedRefreshToken = refreshTokenRepositoryAdapter.save(
                RefreshToken.create(userEntity.getId(), tokenHash, expiresAt)
        );
        springDataRefreshTokenRepository.flush();
        return savedRefreshToken;
    }
}
