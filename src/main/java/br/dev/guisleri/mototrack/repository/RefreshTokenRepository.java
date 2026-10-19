package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    void deleteById(Long id);

}
