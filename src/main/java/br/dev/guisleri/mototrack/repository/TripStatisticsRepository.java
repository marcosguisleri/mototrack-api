package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TripStatisticsRepository {

    // Agregações gerais

    Map<TripStatus, Long> countByOwnerIdAndStatus(Long ownerId);

    double sumDistanceKmByOwnerIdAndStatus(
            Long ownerId,
            TripStatus status
    );

    Map<TerrainType, TerrainStatistics>
    findTerrainStatisticsByOwnerId(Long ownerId);

    List<MonthlyTripStatistics> findMonthlyStatisticsByOwnerIdFromDate(
            Long ownerId,
            LocalDate startDate
    );

    // Destaques de viagens

    Optional<Trip> findLongestCompletedTripByOwnerId(Long ownerId);

    Optional<Trip> findLastCompletedTripByOwnerId(Long ownerId);

    Optional<Trip> findFirstCompletedTripByOwnerId(Long ownerId);

    // Estatísticas por motocicleta

    Map<Motorcycle, Long> countByOwnerIdAndMotorcycle(Long ownerId);

    double sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
            Long ownerId,
            Motorcycle motorcycle,
            TripStatus status
    );

    Optional<Motorcycle> findMostUsedMotorcycleInCompletedTripsByOwnerId(
            Long ownerId
    );

    Long countByOwnerIdAndMotorcycleAndStatus(
            Long ownerId,
            Motorcycle motorcycle,
            TripStatus status
    );

    Optional<Trip> findLongestCompletedTripByOwnerIdAndMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    );

    Optional<Trip> findLastCompletedTripByOwnerIdAndMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    );

    Map<TerrainType, TerrainStatistics> findTerrainStatisticsByOwnerIdAndMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    );

}
