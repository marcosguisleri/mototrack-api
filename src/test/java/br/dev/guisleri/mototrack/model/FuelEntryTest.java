package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidFuelEntryException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuelEntryTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void shouldRegisterFuelEntryWithCreationInstant() {
        FuelEntry fuelEntry = FuelEntry.register(
                7L,
                new BigDecimal("10.2"),
                new BigDecimal("64.90"),
                fixedClock
        );

        assertAll(
                () -> assertNull(fuelEntry.getId()),
                () -> assertEquals(7L, fuelEntry.getTripId()),
                () -> assertEquals(new BigDecimal("10.2"), fuelEntry.getLiters()),
                () -> assertEquals(new BigDecimal("64.90"), fuelEntry.getTotalCost()),
                () -> assertEquals(fixedClock.instant(), fuelEntry.getCreatedAt())
        );
    }

    @Test
    void shouldRejectNonPositiveLiters() {
        assertAll(
                () -> assertThrows(
                        InvalidFuelEntryException.class,
                        () -> FuelEntry.register(
                                7L, BigDecimal.ZERO, new BigDecimal("10"), fixedClock
                        )
                ),
                () -> assertThrows(
                        InvalidFuelEntryException.class,
                        () -> FuelEntry.register(
                                7L, new BigDecimal("-1"), new BigDecimal("10"), fixedClock
                        )
                )
        );
    }

    @Test
    void shouldRejectNonPositiveTotalCost() {
        assertAll(
                () -> assertThrows(
                        InvalidFuelEntryException.class,
                        () -> FuelEntry.register(
                                7L, new BigDecimal("10"), BigDecimal.ZERO, fixedClock
                        )
                ),
                () -> assertThrows(
                        InvalidFuelEntryException.class,
                        () -> FuelEntry.register(
                                7L, new BigDecimal("10"), new BigDecimal("-5"), fixedClock
                        )
                )
        );
    }

    @Test
    void shouldCalculatePricePerLiterRoundedToTwoDecimals() {
        FuelEntry fuelEntry = FuelEntry.register(
                7L,
                new BigDecimal("10.2"),
                new BigDecimal("64.90"),
                fixedClock
        );

        assertEquals(new BigDecimal("6.36"), fuelEntry.pricePerLiter());
    }

    @Test
    void shouldUpdateLitersAndTotalCostKeepingCreationInstant() {
        FuelEntry fuelEntry = FuelEntry.restore(
                1L,
                7L,
                new BigDecimal("10.2"),
                new BigDecimal("631.00"),
                fixedClock.instant()
        );

        fuelEntry.update(new BigDecimal("10.2"), new BigDecimal("63.10"));

        assertAll(
                () -> assertEquals(new BigDecimal("63.10"), fuelEntry.getTotalCost()),
                () -> assertEquals(fixedClock.instant(), fuelEntry.getCreatedAt()),
                () -> assertEquals(1L, fuelEntry.getId())
        );
    }

    @Test
    void shouldRejectInvalidUpdateAndKeepPreviousValues() {
        FuelEntry fuelEntry = FuelEntry.restore(
                1L,
                7L,
                new BigDecimal("10.2"),
                new BigDecimal("64.90"),
                fixedClock.instant()
        );

        assertThrows(
                InvalidFuelEntryException.class,
                () -> fuelEntry.update(BigDecimal.ZERO, new BigDecimal("10"))
        );
        assertAll(
                () -> assertEquals(new BigDecimal("10.2"), fuelEntry.getLiters()),
                () -> assertEquals(new BigDecimal("64.90"), fuelEntry.getTotalCost())
        );
    }
}
