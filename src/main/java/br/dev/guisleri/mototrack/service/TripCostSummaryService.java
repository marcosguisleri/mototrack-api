package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripCostSummary;
import br.dev.guisleri.mototrack.repository.TripCostSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripCostSummaryService {

    private final TripCostSummaryRepository tripCostSummaryRepository;
    private final TripService tripService;

    public TripCostSummaryService(
            TripCostSummaryRepository tripCostSummaryRepository,
            TripService tripService
    ) {
        this.tripCostSummaryRepository = tripCostSummaryRepository;
        this.tripService = tripService;
    }

    @Transactional(readOnly = true)
    public TripCostSummary getCostSummaryForOwner(long tripId, Long ownerId) {
        Trip trip = tripService.findTripByIdForOwner(tripId, ownerId);

        FuelCostSummary fuel =
                tripCostSummaryRepository.summarizeFuelByTripId(tripId);
        List<ExpenseCategorySummary> expenses =
                tripCostSummaryRepository.summarizeExpensesByCategoryForTripId(tripId);

        return TripCostSummary.of(fuel, expenses, trip.getDistanceKm());
    }

}
