package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.VerificationCode;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.entity.VerificationCodeEntity;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeMapper {

    public VerificationCodeEntity toEntity(
            VerificationCode code,
            UserEntity userEntity
    ) {
        return new VerificationCodeEntity(
                code.getId(),
                userEntity,
                code.getPurpose(),
                code.getCodeHash(),
                code.getNewEmail(),
                code.getCreatedAt(),
                code.getExpiresAt(),
                code.getAttempts()
        );
    }

    public VerificationCode toDomain(VerificationCodeEntity entity) {
        return VerificationCode.restore(
                entity.getId(),
                entity.getUser().getId(),
                entity.getPurpose(),
                entity.getCodeHash(),
                entity.getNewEmail(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getAttempts()
        );
    }

}
