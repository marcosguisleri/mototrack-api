package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class TripStatisticsService {

    private final TripRepository tripRepository;

    public TripStatisticsService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public Map<TripStatus, Long> countTripsByStatus(Long ownerId) {
        return tripRepository.countByOwnerIdAndStatus(ownerId);
    }

    public long countCompletedTrips(Long ownerId) {
        return tripRepository.countByOwnerIdAndStatus(ownerId)
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistanceKm(Long ownerId) {
        return tripRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                TripStatus.COMPLETED
        );
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle(Long ownerId) {
        return tripRepository.countByOwnerIdAndMotorcycle(ownerId);
    }

    public double calculateCompletedDistanceKmByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripRepository.sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                ownerId,
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips(
            Long ownerId
    ) {
        return tripRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(
                ownerId
        );
    }

}
