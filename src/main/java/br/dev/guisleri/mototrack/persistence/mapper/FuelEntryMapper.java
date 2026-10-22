package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.persistence.entity.FuelEntryEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.springframework.stereotype.Component;

@Component
public class FuelEntryMapper {

    public FuelEntryEntity toEntity(
            FuelEntry fuelEntry,
            TripEntity tripEntity
    ) {
        return new FuelEntryEntity(
                fuelEntry.getId(),
                tripEntity,
                fuelEntry.getLiters(),
                fuelEntry.getTotalCost(),
                fuelEntry.getCreatedAt()
        );
    }

    public FuelEntry toDomain(FuelEntryEntity fuelEntryEntity) {
        return FuelEntry.restore(
                fuelEntryEntity.getId(),
                fuelEntryEntity.getTrip().getId(),
                fuelEntryEntity.getLiters(),
                fuelEntryEntity.getTotalCost(),
                fuelEntryEntity.getCreatedAt()
        );
    }

}
