package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    private final MotorcycleMapper motorcycleMapper;

    public TripMapper(MotorcycleMapper motorcycleMapper) {
        this.motorcycleMapper = motorcycleMapper;
    }

    public TripEntity toEntity(
            Trip trip,
            MotorcycleEntity motorcycleEntity
    ) {
        return new TripEntity(
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getStatus(),
                trip.getTerrain(),
                trip.getTripDate(),
                motorcycleEntity
        );
    }

    public Trip toDomain(TripEntity entity) {
        return Trip.restore(
                entity.getId(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getDistanceKm(),
                entity.getTerrain(),
                entity.getTripDate(),
                motorcycleMapper.toDomain(entity.getMotorcycle()),
                entity.getStatus()
        );
    }

}
