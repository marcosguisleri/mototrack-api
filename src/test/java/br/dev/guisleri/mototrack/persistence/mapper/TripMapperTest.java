package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TripMapperTest {

    private final UserMapper userMapper = new UserMapper();
    private final MotorcycleMapper motorcycleMapper = new MotorcycleMapper(userMapper);
    private final TripMapper tripMapper = new TripMapper(motorcycleMapper);

    @Test
    void shouldMapTripToEntityWithMotorcycleOwner() {
        User owner = User.restore(1L, "Marcos", "marcos@example.com");
        Motorcycle motorcycle = motorcycle(owner);
        UserEntity ownerEntity = userMapper.toEntity(owner);
        MotorcycleEntity motorcycleEntity = motorcycleMapper.toEntity(
                motorcycle,
                ownerEntity
        );
        Trip trip = trip(motorcycle);

        TripEntity entity = tripMapper.toEntity(trip, motorcycleEntity);

        assertAll(
                () -> assertEquals(42L, entity.getId()),
                () -> assertEquals("Florianopolis", entity.getOrigin()),
                () -> assertEquals("Urubici", entity.getDestination()),
                () -> assertEquals(175.5, entity.getDistanceKm()),
                () -> assertEquals(TripStatus.IN_PROGRESS, entity.getStatus()),
                () -> assertEquals(TerrainType.MIXED, entity.getTerrain()),
                () -> assertEquals(LocalDate.of(2026, 9, 20), entity.getTripDate()),
                () -> assertSame(motorcycleEntity, entity.getMotorcycle()),
                () -> assertSame(ownerEntity, entity.getMotorcycle().getOwner()),
                () -> assertEquals(1L, entity.getMotorcycle().getOwner().getId()),
                () -> assertEquals("Marcos", entity.getMotorcycle().getOwner().getName()),
                () -> assertEquals(
                        "marcos@example.com",
                        entity.getMotorcycle().getOwner().getEmail()
                )
        );
    }

    @Test
    void shouldMapTripEntityToDomainWithMotorcycleOwner() {
        UserEntity ownerEntity = new UserEntity(
                1L,
                "Marcos",
                "marcos@example.com"
        );
        MotorcycleEntity motorcycleEntity = new MotorcycleEntity(
                10L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                ownerEntity
        );
        TripEntity entity = new TripEntity(
                42L,
                "Florianopolis",
                "Urubici",
                175.5,
                TripStatus.IN_PROGRESS,
                TerrainType.MIXED,
                LocalDate.of(2026, 9, 20),
                motorcycleEntity
        );

        Trip trip = tripMapper.toDomain(entity);

        assertAll(
                () -> assertEquals(42L, trip.getId()),
                () -> assertEquals("Florianopolis", trip.getOrigin()),
                () -> assertEquals("Urubici", trip.getDestination()),
                () -> assertEquals(10L, trip.getMotorcycle().getId()),
                () -> assertEquals("Honda", trip.getMotorcycle().getBrand()),
                () -> assertEquals(1L, trip.getMotorcycle().getOwner().getId()),
                () -> assertEquals("Marcos", trip.getMotorcycle().getOwner().getName()),
                () -> assertEquals(
                        "marcos@example.com",
                        trip.getMotorcycle().getOwner().getEmail()
                )
        );
    }

    private Trip trip(Motorcycle motorcycle) {
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

    private Motorcycle motorcycle(User owner) {
        return Motorcycle.restore(
                10L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                owner
        );
    }
}
