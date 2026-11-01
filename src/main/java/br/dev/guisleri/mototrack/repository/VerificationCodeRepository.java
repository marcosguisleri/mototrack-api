package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;

import java.util.Optional;

public interface VerificationCodeRepository {

    VerificationCode save(VerificationCode verificationCode);

    Optional<VerificationCode> findByUserIdAndPurposeForUpdate(
            Long userId,
            VerificationPurpose purpose
    );

    void deleteByUserIdAndPurpose(Long userId, VerificationPurpose purpose);

    void deleteById(Long id);

}
