package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleStatisticsResponseDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleTripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MotorcycleStatisticsService {

    private final MotorcycleService motorcycleService;
    private final TripStatisticsService tripStatisticsService;

    public MotorcycleStatisticsService(MotorcycleService motorcycleService, TripStatisticsService tripStatisticsService) {
        this.motorcycleService = motorcycleService;
        this.tripStatisticsService = tripStatisticsService;
    }

    public MotorcycleStatisticsResponseDTO getStatistics(
            Long ownerId,
            Long motorcycleId
    ) {
        Motorcycle motorcycle =
                motorcycleService.findMotorcycleByIdForOwner(
                        motorcycleId,
                        ownerId
                );

        long completedTrips =
                tripStatisticsService.countCompletedTripsByMotorcycle(
                        ownerId,
                        motorcycle
                );

        double totalCompletedDistanceKm =
                tripStatisticsService.calculateCompletedDistanceKmByMotorcycle(
                        ownerId,
                        motorcycle
                );

        Double averageCompletedDistanceKm =
                completedTrips == 0
                        ? null
                        : totalCompletedDistanceKm / completedTrips;

        MotorcycleTripSummaryResponseDTO longestTrip =
                tripStatisticsService
                        .findLongestCompletedTripByMotorcycle(ownerId, motorcycle)
                        .map(MotorcycleTripSummaryResponseDTO::from)
                        .orElse(null);

        MotorcycleTripSummaryResponseDTO lastCompletedTrip =
                tripStatisticsService
                        .findLastCompletedTripByMotorcycle(ownerId, motorcycle)
                        .map(MotorcycleTripSummaryResponseDTO::from)
                        .orElse(null);

        Map<TerrainType, TerrainStatistics> terrainStatistics =
                tripStatisticsService.findTerrainStatisticsByMotorcycle(
                        ownerId,
                        motorcycle
                );

        return new MotorcycleStatisticsResponseDTO(
                MotorcycleResponseDTO.from(motorcycle),
                completedTrips,
                totalCompletedDistanceKm,
                averageCompletedDistanceKm,
                longestTrip,
                lastCompletedTrip,
                terrainStatistics
        );
    }

}
