package br.dev.guisleri.mototrack.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record TripCostSummary(
        FuelCostSummary fuel,
        List<ExpenseCategorySummary> expenses,
        BigDecimal totalCost,
        BigDecimal costPerKm
) {

    public static TripCostSummary of(
            FuelCostSummary fuel,
            List<ExpenseCategorySummary> expenses,
            double distanceKm
    ) {
        if (fuel.entryCount() == 0 && expenses.isEmpty()) {
            return new TripCostSummary(fuel, expenses, null, null);
        }

        BigDecimal totalCost = fuel.totalCost() != null
                ? fuel.totalCost()
                : BigDecimal.ZERO;

        for (ExpenseCategorySummary expense : expenses) {
            totalCost = totalCost.add(expense.total());
        }

        BigDecimal costPerKm = distanceKm > 0
                ? totalCost.divide(
                        BigDecimal.valueOf(distanceKm),
                        2,
                        RoundingMode.HALF_UP
                )
                : null;

        return new TripCostSummary(fuel, expenses, totalCost, costPerKm);
    }

}
