package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.model.VerificationPurpose;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.entity.VerificationCodeEntity;
import br.dev.guisleri.mototrack.persistence.mapper.VerificationCodeMapper;
import br.dev.guisleri.mototrack.repository.VerificationCodeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaVerificationCodeRepositoryAdapter
        implements VerificationCodeRepository {

    private final SpringDataVerificationCodeRepository verificationCodeRepository;
    private final SpringDataUserRepository userRepository;
    private final VerificationCodeMapper verificationCodeMapper;

    public JpaVerificationCodeRepositoryAdapter(
            SpringDataVerificationCodeRepository verificationCodeRepository,
            SpringDataUserRepository userRepository,
            VerificationCodeMapper verificationCodeMapper
    ) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.verificationCodeMapper = verificationCodeMapper;
    }

    @Override
    @Transactional
    public VerificationCode save(VerificationCode verificationCode) {
        UserEntity userEntity = userRepository
                .findById(verificationCode.getUserId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id %d não encontrado"
                                .formatted(verificationCode.getUserId())
                ));

        VerificationCodeEntity entity =
                verificationCodeMapper.toEntity(verificationCode, userEntity);

        return verificationCodeMapper.toDomain(
                verificationCodeRepository.save(entity)
        );
    }

    @Override
    @Transactional(readOnly = false)
    public Optional<VerificationCode> findByUserIdAndPurposeForUpdate(
            Long userId,
            VerificationPurpose purpose
    ) {
        return verificationCodeRepository
                .findByUserIdAndPurposeForUpdate(userId, purpose)
                .map(verificationCodeMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndPurpose(Long userId, VerificationPurpose purpose) {
        verificationCodeRepository.deleteByUserIdAndPurpose(userId, purpose);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        verificationCodeRepository.deleteById(id);
    }
}
