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

    List<TripEntity> findByTerrain(TerrainType terrain);

    List<TripEntity> findByStatus(TripStatus status);

    List<TripEntity> findByTripDate(LocalDate date);

    List<TripEntity> findByTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
            LocalDate date,
            TripStatus status
    );

    @Query("""
        SELECT t.status AS status, COUNT(t) AS count
        FROM TripEntity t
        GROUP BY t.status
        """)
    List<TripStatusCountProjection> countTripsGroupedByStatus();

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.status = :status
        """)
    double sumDistanceByStatus(
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(t.distanceKm), 0)
        FROM TripEntity t
        WHERE t.motorcycle.id = :motorcycleId
        AND t.status = :status
        """)
    double sumDistanceByMotorcycleIdAndStatus(
            @Param("motorcycleId") long motorcycleId,
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS count
        FROM TripEntity t
        WHERE t.status = :status
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycleAndStatus(
            @Param("status") TripStatus status
    );

    @Query("""
        SELECT t.motorcycle AS motorcycle, COUNT(t) AS count
        FROM TripEntity t
        GROUP BY t.motorcycle
        """)
    List<MotorcycleTripCountProjection> countTripsGroupedByMotorcycle();

}
