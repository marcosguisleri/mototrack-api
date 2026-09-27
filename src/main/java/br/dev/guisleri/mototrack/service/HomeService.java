package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.HomeResponseDTO;
import br.dev.guisleri.mototrack.dto.NextTripResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.Trip;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public HomeResponseDTO getHome(Long ownerId) {

        NextTripResponseDTO nextTrip =
                findNextTrip(ownerId);

        TripSummaryResponseDTO lastCompletedTrip =
                tripStatisticsService.findLastCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        Double totalCompletedDistanceKm =
                tripStatisticsService.calculateTotalCompletedDistanceKm(ownerId);

        Long completedTrips =
                tripStatisticsService.countCompletedTrips(ownerId);

        return new HomeResponseDTO(
                nextTrip,
                lastCompletedTrip,
                totalCompletedDistanceKm,
                completedTrips,
                countMotorcycles(ownerId)
        );
    }

    private NextTripResponseDTO findNextTrip(Long ownerId) {
        Optional<Trip> trip =
                tripService.findUpcomingTripsByOwnerId(ownerId)
                        .stream()
                        .findFirst();

        if (trip.isEmpty()) {
            return null;
        }

        long daysUntil =
                tripService.calculateDaysUntilTripForOwner(
                        trip.get().getId(),
                        ownerId
                );

        return NextTripResponseDTO.from(
                trip.get(),
                daysUntil
        );
    }

    private long countMotorcycles(Long ownerId) {
        return motorcycleService
                .findMotorcyclesByOwnerId(ownerId)
                .size();
    }
}