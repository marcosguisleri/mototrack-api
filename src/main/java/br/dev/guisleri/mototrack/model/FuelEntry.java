package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidFuelEntryException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

public class FuelEntry {

    private final Long id;
    private final Long tripId;
    private BigDecimal liters;
    private BigDecimal totalCost;
    private final Instant createdAt;

    private FuelEntry(
            Long id,
            Long tripId,
            BigDecimal liters,
            BigDecimal totalCost,
            Instant createdAt
    ) {
        this.id = id;
        this.tripId = tripId;
        this.liters = liters;
        this.totalCost = totalCost;
        this.createdAt = createdAt;
    }

    public static FuelEntry register(
            Long tripId,
            BigDecimal liters,
            BigDecimal totalCost,
            Clock clock
    ) {
        requirePositive(liters, totalCost);

        return new FuelEntry(null, tripId, liters, totalCost, clock.instant());
    }

    public static FuelEntry restore(
            Long id,
            Long tripId,
            BigDecimal liters,
            BigDecimal totalCost,
            Instant createdAt
    ) {
        return new FuelEntry(id, tripId, liters, totalCost, createdAt);
    }

    public void update(BigDecimal liters, BigDecimal totalCost) {
        requirePositive(liters, totalCost);

        this.liters = liters;
        this.totalCost = totalCost;
    }

    public BigDecimal pricePerLiter() {
        return totalCost.divide(liters, 2, RoundingMode.HALF_UP);
    }

    private static void requirePositive(BigDecimal liters, BigDecimal totalCost) {
        if (liters.signum() <= 0) {
            throw new InvalidFuelEntryException(
                    "Os litros abastecidos devem ser maiores que zero."
            );
        }

        if (totalCost.signum() <= 0) {
            throw new InvalidFuelEntryException(
                    "O valor pago deve ser maior que zero."
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
