package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.persistence.entity.RefreshTokenEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshTokenEntity toEntity(
            RefreshToken token,
            UserEntity userEntity
    ) {
        return new RefreshTokenEntity(
                token.getId(),
                userEntity,
                token.getTokenHash(),
                token.getExpiresAt()
        );
    }

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.restore(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTokenHash(),
                entity.getExpiresAt()
        );
    }

}
