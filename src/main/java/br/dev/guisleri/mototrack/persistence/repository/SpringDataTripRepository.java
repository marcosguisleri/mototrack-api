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
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS tripCount
        FROM TripEntity t
        WHERE t.motorcycle.owner.id = :ownerId
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycleForOwner(
            @Param("ownerId") Long ownerId
    );

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
