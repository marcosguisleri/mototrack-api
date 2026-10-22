package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.FuelEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record FuelEntryResponseDTO(
        long id,
        BigDecimal liters,
        BigDecimal totalCost,
        BigDecimal pricePerLiter,
        Instant createdAt
) {
    public static FuelEntryResponseDTO from(FuelEntry fuelEntry) {
        return new FuelEntryResponseDTO(
                fuelEntry.getId(),
                fuelEntry.getLiters(),
                fuelEntry.getTotalCost(),
                fuelEntry.pricePerLiter(),
                fuelEntry.getCreatedAt()
        );
    }
}
