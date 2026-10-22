package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;
import br.dev.guisleri.mototrack.persistence.projection.FuelCostProjection;
import br.dev.guisleri.mototrack.repository.TripCostSummaryRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class JpaTripCostSummaryRepositoryAdapter
        implements TripCostSummaryRepository {

    private final SpringDataTripCostSummaryRepository springDataTripCostSummaryRepository;

    public JpaTripCostSummaryRepositoryAdapter(
            SpringDataTripCostSummaryRepository springDataTripCostSummaryRepository
    ) {
        this.springDataTripCostSummaryRepository = springDataTripCostSummaryRepository;
    }

    @Override
    public FuelCostSummary summarizeFuelByTripId(long tripId) {
        FuelCostProjection projection =
                springDataTripCostSummaryRepository.summarizeFuelByTripId(tripId);

        return new FuelCostSummary(
                projection.getEntryCount(),
                projection.getLiters(),
                projection.getTotalCost()
        );
    }

    @Override
    public List<ExpenseCategorySummary> summarizeExpensesByCategoryForTripId(
            long tripId
    ) {
        return springDataTripCostSummaryRepository
                .summarizeExpensesByCategoryForTripId(tripId)
                .stream()
                .map(projection -> new ExpenseCategorySummary(
                        projection.getCategory(),
                        projection.getExpenseCount(),
                        projection.getTotal()
                ))
                .sorted(Comparator.comparing(ExpenseCategorySummary::category))
                .toList();
    }

}
