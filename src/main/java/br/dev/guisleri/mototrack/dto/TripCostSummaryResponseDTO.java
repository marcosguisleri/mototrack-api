package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.TripCostSummary;

import java.math.BigDecimal;
import java.util.List;

public record TripCostSummaryResponseDTO(
        FuelCost fuel,
        List<ExpenseCategoryCost> expenses,
        BigDecimal totalCost,
        BigDecimal costPerKm
) {

    public record FuelCost(
            long entries,
            BigDecimal liters,
            BigDecimal totalCost
    ) {
    }

    public record ExpenseCategoryCost(
            ExpenseCategory category,
            long count,
            BigDecimal total
    ) {
    }

    public static TripCostSummaryResponseDTO from(TripCostSummary summary) {
        return new TripCostSummaryResponseDTO(
                new FuelCost(
                        summary.fuel().entryCount(),
                        summary.fuel().liters(),
                        summary.fuel().totalCost()
                ),
                summary.expenses().stream()
                        .map(expense -> new ExpenseCategoryCost(
                                expense.category(),
                                expense.count(),
                                expense.total()
                        ))
                        .toList(),
                summary.totalCost(),
                summary.costPerKm()
        );
    }
}
