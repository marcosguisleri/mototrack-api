package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.persistence.entity.FuelEntryEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FuelEntryMapperTest {

    private final FuelEntryMapper fuelEntryMapper = new FuelEntryMapper();

    @Test
    void shouldMapFuelEntryToEntity() {
        Instant createdAt = Instant.parse("2026-09-14T12:00:00Z");
        FuelEntry fuelEntry = FuelEntry.restore(
                3L, 42L, new BigDecimal("10.2"), new BigDecimal("64.90"), createdAt
        );
        TripEntity tripEntity = new TripEntity(
                42L, "A", "B", 10.0, null, null, null, null, null, null
        );

        FuelEntryEntity entity = fuelEntryMapper.toEntity(fuelEntry, tripEntity);

        assertAll(
                () -> assertEquals(3L, entity.getId()),
                () -> assertSame(tripEntity, entity.getTrip()),
                () -> assertEquals(new BigDecimal("10.2"), entity.getLiters()),
                () -> assertEquals(new BigDecimal("64.90"), entity.getTotalCost()),
                () -> assertEquals(createdAt, entity.getCreatedAt())
        );
    }

    @Test
    void shouldMapEntityToDomainWithTripId() {
        Instant createdAt = Instant.parse("2026-09-14T12:00:00Z");
        TripEntity tripEntity = new TripEntity(
                42L, "A", "B", 10.0, null, null, null, null, null, null
        );
        FuelEntryEntity entity = new FuelEntryEntity(
                3L, tripEntity, new BigDecimal("10.2"), new BigDecimal("64.90"), createdAt
        );

        FuelEntry fuelEntry = fuelEntryMapper.toDomain(entity);

        assertAll(
                () -> assertEquals(3L, fuelEntry.getId()),
                () -> assertEquals(42L, fuelEntry.getTripId()),
                () -> assertEquals(new BigDecimal("10.2"), fuelEntry.getLiters()),
                () -> assertEquals(new BigDecimal("64.90"), fuelEntry.getTotalCost()),
                () -> assertEquals(createdAt, fuelEntry.getCreatedAt())
        );
    }
}
