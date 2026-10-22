package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;

import java.util.List;

public interface TripCostSummaryRepository {

    FuelCostSummary summarizeFuelByTripId(long tripId);

    List<ExpenseCategorySummary> summarizeExpensesByCategoryForTripId(long tripId);

}
