package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.util.Map;
import java.util.Optional;

public interface TripStatisticsRepository {

    Map<TripStatus, Long> countByOwnerIdAndStatus(Long ownerId);

    double sumDistanceKmByOwnerIdAndStatus(
            Long ownerId,
            TripStatus status
    );

    Map<Motorcycle, Long> countByOwnerIdAndMotorcycle(Long ownerId);

    double sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
            Long ownerId,
            Motorcycle motorcycle,
            TripStatus status
    );

    Optional<Motorcycle> findMostUsedMotorcycleInCompletedTripsByOwnerId(
            Long ownerId
    );
}
