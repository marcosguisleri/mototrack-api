package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.projection.MotorcycleTripCountProjection;
import br.dev.guisleri.mototrack.persistence.projection.TripStatusCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataTripRepository extends JpaRepository<TripEntity, Long> {

    @Query("""
        SELECT t.status AS status, COUNT(t) AS tripCount
        FROM TripEntity t
        GROUP BY t.status
        """)
    List<TripStatusCountProjection> countTripsGroupedByStatus();

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.status = :status
        """)
    double sumDistanceKmByStatus(
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.motorcycle.id = :motorcycleId
        AND t.status = :status
        """)
    double sumDistanceKmByMotorcycleIdAndStatus(
            @Param("motorcycleId") long motorcycleId,
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS tripCount
        FROM TripEntity t
        WHERE t.status = :status
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycleAndStatus(
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS tripCount
        FROM TripEntity t
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycle();

    List<TripEntity> findByMotorcycle_Owner_Id(Long ownerId);

    List<TripEntity> findByMotorcycle_Owner_IdAndStatus(Long ownerId, TripStatus status);

    List<TripEntity> findByMotorcycle_Owner_IdAndTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
            Long ownerId,
            LocalDate startDate,
            TripStatus status
    );

    List<TripEntity> findByMotorcycle_Owner_IdAndTerrain(
            Long ownerId,
            TerrainType terrainType
    );

    List<TripEntity> findByMotorcycle_Owner_IdAndTripDate(
            Long ownerId,
            LocalDate tripDate
    );

    boolean existsByMotorcycleId(Long motorcycleId);

}
