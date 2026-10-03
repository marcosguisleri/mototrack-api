package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.persistence.entity.RefreshTokenEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.RefreshTokenMapper;
import br.dev.guisleri.mototrack.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaRefreshTokenRepositoryAdapter
        implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final SpringDataUserRepository userRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    public JpaRefreshTokenRepositoryAdapter(
            SpringDataRefreshTokenRepository refreshTokenRepository,
            SpringDataUserRepository userRepository,
            RefreshTokenMapper refreshTokenMapper
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        UserEntity userEntity = userRepository
                .findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(
                "Usuário com id %d não encontrado"
                        .formatted(refreshToken.getUserId())
        ));

        RefreshTokenEntity entity =
                refreshTokenMapper.toEntity(refreshToken, userEntity);

        RefreshTokenEntity savedEntity =
                refreshTokenRepository.save(entity);

        return refreshTokenMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenRepository
                .findByTokenHash(tokenHash)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        refreshTokenRepository.deleteById(id);
    }
}
