package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class MotorcycleMapper {

    private final UserMapper userMapper;

    public MotorcycleMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public MotorcycleEntity toEntity(
            Motorcycle motorcycle,
            UserEntity userEntity
    ) {
        return new MotorcycleEntity(
                motorcycle.getId(),
                motorcycle.getBrand(),
                motorcycle.getModel(),
                motorcycle.getColor(),
                motorcycle.getYear(),
                motorcycle.getEngineCapacity(),
                userEntity
        );
    }

    public Motorcycle toDomain(MotorcycleEntity motorcycleEntity) {
        return Motorcycle.restore(
                motorcycleEntity.getId(),
                motorcycleEntity.getBrand(),
                motorcycleEntity.getModel(),
                motorcycleEntity.getColor(),
                motorcycleEntity.getYear(),
                motorcycleEntity.getEngineCapacity(),
                userMapper.toDomain(motorcycleEntity.getOwner())
        );
    }
}
