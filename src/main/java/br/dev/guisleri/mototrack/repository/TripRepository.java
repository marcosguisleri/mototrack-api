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

    void save(Trip trip);

    Optional<Trip> findById(long id);

    List<Trip> findAll();

    List<Trip> findByTerrain(TerrainType terrainType);

    List<Trip> findByStatus(TripStatus status);

    Map<TripStatus, Long> countByStatus();

    List<Trip> findByTripDate(LocalDate date);

    List<Trip> findUpcomingFrom(LocalDate date);

    double sumDistanceByStatus(TripStatus status);

    Map<Motorcycle, Long> countByMotorcycle();

    double sumDistanceByMotorcycleAndStatus(
            Motorcycle motorcycle,
            TripStatus status
    );

    Optional<Motorcycle> findMostUsedMotorcycle();

}
