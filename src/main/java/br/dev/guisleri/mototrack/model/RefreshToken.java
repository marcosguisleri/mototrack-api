package br.dev.guisleri.mototrack.model;

import java.time.Instant;

public class RefreshToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final Instant expiresAt;

    private RefreshToken(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(
            Long userId,
            String tokenHash,
            Instant expiresAt
    ) {
        return new RefreshToken(
                null,
                userId,
                tokenHash,
                expiresAt
        );
    }

    public static RefreshToken restore(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt
    ) {
        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt
        );
    }


    public boolean isExpired(Instant currentInstant) {
        return !expiresAt.isAfter(currentInstant);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
