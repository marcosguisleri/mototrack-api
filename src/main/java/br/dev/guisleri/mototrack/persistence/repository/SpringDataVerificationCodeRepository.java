package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.persistence.entity.VerificationCodeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataVerificationCodeRepository
        extends JpaRepository<VerificationCodeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM VerificationCodeEntity c
            WHERE c.user.id = :userId AND c.purpose = :purpose
            """)
    Optional<VerificationCodeEntity> findByUserIdAndPurposeForUpdate(
            @Param("userId") Long userId,
            @Param("purpose") VerificationPurpose purpose
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM VerificationCodeEntity c
            WHERE c.user.id = :userId AND c.purpose = :purpose
            """)
    void deleteByUserIdAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") VerificationPurpose purpose
    );
}
