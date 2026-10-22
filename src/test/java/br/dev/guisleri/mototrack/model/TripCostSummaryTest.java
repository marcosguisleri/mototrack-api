package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TripCostSummaryTest {

    private static final FuelCostSummary NO_FUEL = new FuelCostSummary(0, null, null);

    @Test
    void shouldHaveNoTotalsWhenNothingWasRecorded() {
        TripCostSummary summary = TripCostSummary.of(NO_FUEL, List.of(), 170.0);

        assertAll(
                () -> assertEquals(0, summary.fuel().entryCount()),
                () -> assertNull(summary.fuel().liters()),
                () -> assertNull(summary.fuel().totalCost()),
                () -> assertEquals(List.of(), summary.expenses()),
                () -> assertNull(summary.totalCost()),
                () -> assertNull(summary.costPerKm())
        );
    }

    @Test
    void shouldSumFuelAndExpensesAndCalculateCostPerKm() {
        FuelCostSummary fuel = new FuelCostSummary(
                2, new BigDecimal("16.2"), new BigDecimal("103.90")
        );
        List<ExpenseCategorySummary> expenses = List.of(
                new ExpenseCategorySummary(
                        ExpenseCategory.TOLL, 3, new BigDecimal("37.80")
                ),
                new ExpenseCategorySummary(
                        ExpenseCategory.FOOD, 1, new BigDecimal("42.01")
                )
        );

        TripCostSummary summary = TripCostSummary.of(fuel, expenses, 170.0);

        assertAll(
                () -> assertEquals(new BigDecimal("183.71"), summary.totalCost()),
                () -> assertEquals(new BigDecimal("1.08"), summary.costPerKm())
        );
    }

    @Test
    void shouldTotalOnlyExpensesWhenThereIsNoFuel() {
        List<ExpenseCategorySummary> expenses = List.of(
                new ExpenseCategorySummary(
                        ExpenseCategory.TOLL, 1, new BigDecimal("13.90")
                )
        );

        TripCostSummary summary = TripCostSummary.of(NO_FUEL, expenses, 100.0);

        assertAll(
                () -> assertEquals(new BigDecimal("13.90"), summary.totalCost()),
                () -> assertEquals(new BigDecimal("0.14"), summary.costPerKm()),
                () -> assertNull(summary.fuel().totalCost())
        );
    }

    @Test
    void shouldTotalOnlyFuelWhenThereAreNoExpenses() {
        FuelCostSummary fuel = new FuelCostSummary(
                1, new BigDecimal("10"), new BigDecimal("60.00")
        );

        TripCostSummary summary = TripCostSummary.of(fuel, List.of(), 120.0);

        assertAll(
                () -> assertEquals(new BigDecimal("60.00"), summary.totalCost()),
                () -> assertEquals(new BigDecimal("0.50"), summary.costPerKm())
        );
    }

    @Test
    void shouldHaveNoCostPerKmWhenDistanceIsZero() {
        FuelCostSummary fuel = new FuelCostSummary(
                1, new BigDecimal("10"), new BigDecimal("60.00")
        );

        TripCostSummary summary = TripCostSummary.of(fuel, List.of(), 0.0);

        assertAll(
                () -> assertEquals(new BigDecimal("60.00"), summary.totalCost()),
                () -> assertNull(summary.costPerKm())
        );
    }
}
