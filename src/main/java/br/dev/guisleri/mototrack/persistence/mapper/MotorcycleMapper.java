package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import org.springframework.stereotype.Component;

@Component
public class MotorcycleMapper {

    public MotorcycleEntity toEntity(Motorcycle motorcycle) {
        return new MotorcycleEntity(
                motorcycle.getId(),
                motorcycle.getBrand(),
                motorcycle.getModel(),
                motorcycle.getColor(),
                motorcycle.getYear(),
                motorcycle.getEngineCapacity()
        );
    }

    public Motorcycle toDomain(MotorcycleEntity motorcycleEntity) {
        return Motorcycle.restore(
                motorcycleEntity.getId(),
                motorcycleEntity.getBrand(),
                motorcycleEntity.getModel(),
                motorcycleEntity.getColor(),
                motorcycleEntity.getYear(),
                motorcycleEntity.getEngineCapacity()
        );
    }
}
