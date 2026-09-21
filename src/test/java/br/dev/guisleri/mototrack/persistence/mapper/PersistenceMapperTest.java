package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PersistenceMapperTest {

    private final MotorcycleMapper motorcycleMapper = new MotorcycleMapper();
    private final TripMapper tripMapper = new TripMapper(motorcycleMapper);

    @Test
    void shouldMapMotorcycleToEntityAndBackToDomain() {
        Motorcycle motorcycle = motorcycle();

        MotorcycleEntity entity = motorcycleMapper.toEntity(motorcycle);
        Motorcycle restoredMotorcycle = motorcycleMapper.toDomain(entity);

        assertAll(
                () -> assertEquals(motorcycle.getId(), entity.getId()),
                () -> assertEquals(motorcycle.getBrand(), entity.getBrand()),
                () -> assertEquals(motorcycle.getModel(), entity.getModel()),
                () -> assertEquals(motorcycle.getColor(), entity.getColor()),
                () -> assertEquals(motorcycle.getYear(), entity.getYear()),
                () -> assertEquals(motorcycle.getEngineCapacity(), entity.getEngineCapacity()),
                () -> assertEquals(motorcycle.getId(), restoredMotorcycle.getId()),
                () -> assertEquals(motorcycle.getBrand(), restoredMotorcycle.getBrand()),
                () -> assertEquals(motorcycle.getModel(), restoredMotorcycle.getModel()),
                () -> assertEquals(motorcycle.getColor(), restoredMotorcycle.getColor()),
                () -> assertEquals(motorcycle.getYear(), restoredMotorcycle.getYear()),
                () -> assertEquals(
                        motorcycle.getEngineCapacity(),
                        restoredMotorcycle.getEngineCapacity()
                )
        );
    }

    @Test
    void shouldMapTripToEntity() {
        Motorcycle motorcycle = motorcycle();
        MotorcycleEntity motorcycleEntity = motorcycleMapper.toEntity(motorcycle);
        Trip trip = restoredTrip(motorcycle);

        TripEntity entity = tripMapper.toEntity(trip, motorcycleEntity);

        assertAll(
                () -> assertEquals(42L, entity.getId()),
                () -> assertEquals("Florianopolis", entity.getOrigin()),
                () -> assertEquals("Urubici", entity.getDestination()),
                () -> assertEquals(175.5, entity.getDistanceKm()),
                () -> assertEquals(TripStatus.IN_PROGRESS, entity.getStatus()),
                () -> assertEquals(TerrainType.MIXED, entity.getTerrain()),
                () -> assertEquals(LocalDate.of(2026, 9, 20), entity.getTripDate()),
                () -> assertSame(motorcycleEntity, entity.getMotorcycle())
        );
    }

    @Test
    void shouldRestoreTripFromEntity() {
        Motorcycle motorcycle = motorcycle();
        TripEntity entity = tripMapper.toEntity(
                restoredTrip(motorcycle),
                motorcycleMapper.toEntity(motorcycle)
        );

        Trip restoredTrip = tripMapper.toDomain(entity);

        assertAll(
                () -> assertEquals(42L, restoredTrip.getId()),
                () -> assertEquals("Florianopolis", restoredTrip.getOrigin()),
                () -> assertEquals("Urubici", restoredTrip.getDestination()),
                () -> assertEquals(175.5, restoredTrip.getDistanceKm()),
                () -> assertEquals(TripStatus.IN_PROGRESS, restoredTrip.getStatus()),
                () -> assertEquals(TerrainType.MIXED, restoredTrip.getTerrain()),
                () -> assertEquals(LocalDate.of(2026, 9, 20), restoredTrip.getTripDate()),
                () -> assertEquals(motorcycle, restoredTrip.getMotorcycle()),
                () -> assertEquals("Honda", restoredTrip.getMotorcycle().getBrand()),
                () -> assertEquals("NX 500", restoredTrip.getMotorcycle().getModel())
        );
    }

    private Trip restoredTrip(Motorcycle motorcycle) {
        return Trip.restore(
                42L,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                LocalDate.of(2026, 9, 20),
                motorcycle,
                TripStatus.IN_PROGRESS
        );
    }

    private Motorcycle motorcycle() {
        return Motorcycle.restore(1L, "Honda", "NX 500", "Black", 2025, 471);
    }
}
