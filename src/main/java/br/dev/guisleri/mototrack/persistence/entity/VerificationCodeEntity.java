package br.dev.guisleri.mototrack.persistence.entity;

import br.dev.guisleri.mototrack.model.VerificationPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "verification_codes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "purpose"})
)
public class VerificationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "new_email")
    private String newEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    protected VerificationCodeEntity() {
    }

    public VerificationCodeEntity(
            Long id,
            UserEntity user,
            VerificationPurpose purpose,
            String codeHash,
            String newEmail,
            Instant createdAt,
            Instant expiresAt,
            int attempts
    ) {
        this.id = id;
        this.user = user;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.newEmail = newEmail;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
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
