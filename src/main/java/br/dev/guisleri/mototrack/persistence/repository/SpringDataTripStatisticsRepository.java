package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.projection.MonthlyTripStatisticsProjection;
import br.dev.guisleri.mototrack.persistence.projection.MotorcycleTripCountProjection;
import br.dev.guisleri.mototrack.persistence.projection.TerrainTripStatisticsProjection;
import br.dev.guisleri.mototrack.persistence.projection.TripStatusCountProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataTripStatisticsRepository
        extends Repository<TripEntity, Long> {

    // Agregações gerais

    @Query("""
        SELECT t.status AS status, COUNT(t) AS tripCount
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        GROUP BY t.status
        """)
    List<TripStatusCountProjection> countTripsGroupedByStatusForOwner(
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        AND t.status = :status
        """)
    double sumDistanceKmByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT
            t.terrain AS terrain,
            COUNT(t) AS tripCount,
            COALESCE(SUM(t.distanceKm), 0) AS totalDistanceKm
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        AND t.status = :status
        GROUP BY t.terrain
        """)
    List<TerrainTripStatisticsProjection> findTerrainStatisticsByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT
            YEAR(t.tripDate) AS year,
            MONTH(t.tripDate) AS month,
            COUNT(t) AS tripCount,
            COALESCE(SUM(t.distanceKm), 0) AS totalDistanceKm
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        AND t.status = :status
        AND t.tripDate >= :startDate
        GROUP BY YEAR(t.tripDate), MONTH(t.tripDate)
        ORDER BY YEAR(t.tripDate), MONTH(t.tripDate)
        """)
    List<MonthlyTripStatisticsProjection>
    findMonthlyStatisticsByOwnerIdAndStatusFromDate(
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status,
            @Param("startDate") LocalDate startDate
    );

    // Destaques de viagens

    Optional<TripEntity>
    findFirstByMotorcycle_Owner_IdAndStatusOrderByDistanceKmDescTripDateDescIdDesc(
            Long ownerId,
            TripStatus status
    );

    Optional<TripEntity>
    findFirstByMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
            Long ownerId,
            TripStatus status
    );

    Optional<TripEntity>
    findFirstByMotorcycle_Owner_IdAndStatusOrderByTripDateAscIdAsc(
            Long ownerId,
            TripStatus status
    );

    // Estatísticas por motocicleta

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS tripCount
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycleForOwner(
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.motorcycle.id = :motorcycleId
        AND t.motorcycle.owner.id = :ownerId
        AND t.status = :status
        """)
    double sumDistanceKmByMotorcycleIdAndOwnerIdAndStatus(
            @Param("motorcycleId") long motorcycleId,
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS tripCount
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        AND t.status = :status
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycleAndOwnerAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status
    );

    @Query("""
    SELECT
        t.terrain AS terrain,
        COUNT(t) AS tripCount,
        COALESCE(SUM(t.distanceKm), 0) AS totalDistanceKm
    FROM TripEntity t
    WHERE t.motorcycle.id = :motorcycleId
    AND t.motorcycle.owner.id = :ownerId
    AND t.status = :status
    GROUP BY t.terrain
    """)
    List<TerrainTripStatisticsProjection> findTerrainStatisticsByMotorcycleIdAndOwnerIdAndStatus(
            @Param("motorcycleId") Long motorcycleId,
            @Param("ownerId") Long ownerId,
            @Param("status") TripStatus status
    );

    long countByMotorcycle_IdAndMotorcycle_Owner_IdAndStatus(
            Long motorcycleId,
            Long ownerId,
            TripStatus status
    );

    Optional<TripEntity>
    findFirstByMotorcycle_IdAndMotorcycle_Owner_IdAndStatusOrderByDistanceKmDescTripDateDescIdDesc(
            Long motorcycleId,
            Long ownerId,
            TripStatus status
    );

    Optional<TripEntity>
    findFirstByMotorcycle_IdAndMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
            Long motorcycleId,
            Long ownerId,
            TripStatus status
    );

}
