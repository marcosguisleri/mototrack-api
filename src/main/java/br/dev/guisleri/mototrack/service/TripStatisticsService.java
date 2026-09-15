package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class TripStatisticsService {

    private final TripRepository repository;

    public TripStatisticsService(TripRepository repository) {
        this.repository = repository;
    }

    public Map<TripStatus, Long> countTripsByStatus() {
        return repository.countByStatus();
    }

    public long countCompletedTrips() {
        return repository.countByStatus()
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistance() {
        return repository.sumDistanceByStatus(TripStatus.COMPLETED);
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle() {
        return repository.countByMotorcycle();
    }

    public double calculateCompletedDistanceByMotorcycle(
            Motorcycle motorcycle
    ) {
        return repository.sumDistanceByMotorcycleAndStatus(
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public Optional<Motorcycle> getMostUsedMotorcycleInCompletedTrips() {
        return repository.findMostUsedMotorcycleInCompletedTrips();
    }

}
