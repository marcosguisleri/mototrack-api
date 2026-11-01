package br.dev.guisleri.mototrack.model;

import java.time.Instant;

public class VerificationCode {

    public static final int MAX_ATTEMPTS = 5;

    private final Long id;
    private final Long userId;
    private final VerificationPurpose purpose;
    private final String codeHash;
    private final String newEmail;
    private final Instant createdAt;
    private final Instant expiresAt;
    private int attempts;

    private VerificationCode(
            Long id,
            Long userId,
            VerificationPurpose purpose,
            String codeHash,
            String newEmail,
            Instant createdAt,
            Instant expiresAt,
            int attempts
    ) {
        this.id = id;
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.newEmail = newEmail;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
    }

    public static VerificationCode create(
            Long userId,
            VerificationPurpose purpose,
            String codeHash,
            String newEmail,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new VerificationCode(
                null,
                userId,
                purpose,
                codeHash,
                newEmail,
                createdAt,
                expiresAt,
                0
        );
    }

    public static VerificationCode restore(
            Long id,
            Long userId,
            VerificationPurpose purpose,
            String codeHash,
            String newEmail,
            Instant createdAt,
            Instant expiresAt,
            int attempts
    ) {
        return new VerificationCode(
                id,
                userId,
                purpose,
                codeHash,
                newEmail,
                createdAt,
                expiresAt,
                attempts
        );
    }

    public boolean isExpired(Instant currentInstant) {
        return !expiresAt.isAfter(currentInstant);
    }

    public boolean hasAttemptsLeft() {
        return attempts < MAX_ATTEMPTS;
    }

    public void registerFailedAttempt() {
        attempts++;
    }

    public boolean wasCreatedAfter(Instant instant) {
        return createdAt.isAfter(instant);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public VerificationPurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
