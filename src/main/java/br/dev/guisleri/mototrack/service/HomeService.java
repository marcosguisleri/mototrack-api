package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.HomeResponseDTO;
import br.dev.guisleri.mototrack.dto.NextTripResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.Trip;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

    private final TripService tripService;
    private final TripStatisticsService tripStatisticsService;
    private final MotorcycleService motorcycleService;

    public HomeService(
            TripService tripService,
            TripStatisticsService tripStatisticsService,
            MotorcycleService motorcycleService
    ) {
        this.tripService = tripService;
        this.tripStatisticsService = tripStatisticsService;
        this.motorcycleService = motorcycleService;
    }

    @Transactional(readOnly = true)
    public HomeResponseDTO getHome(Long ownerId) {
        TripSummaryResponseDTO inProgressTrip =
                tripService.findInProgressTripByOwnerId(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        NextTripResponseDTO nextTrip = findNextTrip(ownerId);

        TripSummaryResponseDTO lastCompletedTrip =
                tripStatisticsService.findLastCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        double totalCompletedDistanceKm =
                tripStatisticsService.calculateTotalCompletedDistanceKm(ownerId);

        double distanceThisYearKm =
                tripStatisticsService
                        .calculateCompletedDistanceKmForCurrentYear(ownerId);

        long completedTrips =
                tripStatisticsService.countCompletedTrips(ownerId);

        long motorcycleCount =
                motorcycleService.countMotorcyclesByOwnerId(ownerId);

        return new HomeResponseDTO(
                inProgressTrip,
                nextTrip,
                lastCompletedTrip,
                totalCompletedDistanceKm,
                distanceThisYearKm,
                completedTrips,
                motorcycleCount
        );
    }

    private NextTripResponseDTO findNextTrip(Long ownerId) {
        return tripService.findNextUpcomingTripByOwnerId(ownerId)
                .map(trip -> NextTripResponseDTO.from(
                        trip,
                        tripService.calculateDaysUntilTrip(trip)
                ))
                .orElse(null);
    }
}
