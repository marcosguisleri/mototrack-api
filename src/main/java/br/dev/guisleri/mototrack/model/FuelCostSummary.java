package br.dev.guisleri.mototrack.model;

import java.math.BigDecimal;

public record FuelCostSummary(
        long entryCount,
        BigDecimal liters,
        BigDecimal totalCost
) {
}
