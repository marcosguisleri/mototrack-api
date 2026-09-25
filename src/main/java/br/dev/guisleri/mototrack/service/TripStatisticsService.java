package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class TripStatisticsService {

    private final TripStatisticsRepository tripStatisticsRepository;

    public TripStatisticsService(TripStatisticsRepository tripStatisticsRepository) {
        this.tripStatisticsRepository = tripStatisticsRepository;
    }

    public Map<TripStatus, Long> countTripsByStatus(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndStatus(ownerId);
    }

    public long countCompletedTrips(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndStatus(ownerId)
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistanceKm(Long ownerId) {
        return tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                TripStatus.COMPLETED
        );
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndMotorcycle(ownerId);
    }

    public double calculateCompletedDistanceKmByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository.sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                ownerId,
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips(
            Long ownerId
    ) {
        return tripStatisticsRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(
                ownerId
        );
    }

}
