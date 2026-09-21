package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TripRepository {

    Trip save(Trip trip);

    Optional<Trip> findById(long tripId);

    List<Trip> findAll();

    List<Trip> findByTerrain(TerrainType terrainType);

    List<Trip> findByStatus(TripStatus status);

    Map<TripStatus, Long> countByStatus();

    List<Trip> findByTripDate(LocalDate tripDate);

    List<Trip> findUpcomingFrom(LocalDate referenceDate);

    double sumDistanceKmByStatus(TripStatus status);

    Map<Motorcycle, Long> countByMotorcycle();

    double sumDistanceKmByMotorcycleAndStatus(
            Motorcycle motorcycle,
            TripStatus status
    );

    Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips();

    boolean existsByMotorcycleId(Long motorcycleId);

    void deleteById(Long tripId);

}
