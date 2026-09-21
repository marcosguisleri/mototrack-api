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

    public Map<TripStatus, Long> countTripsByStatus() {
        return tripRepository.countByStatus();
    }

    public long countCompletedTrips() {
        return tripRepository.countByStatus()
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistanceKm() {
        return tripRepository.sumDistanceKmByStatus(TripStatus.COMPLETED);
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle() {
        return tripRepository.countByMotorcycle();
    }

    public double calculateCompletedDistanceKmByMotorcycle(
            Motorcycle motorcycle
    ) {
        return tripRepository.sumDistanceKmByMotorcycleAndStatus(
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips() {
        return tripRepository.findMostUsedMotorcycleInCompletedTrips();
    }

}
