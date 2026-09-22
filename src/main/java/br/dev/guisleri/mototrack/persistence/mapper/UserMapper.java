package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public User toDomain(UserEntity userEntity) {
        return User.restore(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getEmail()
        );
    }

}
