package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataTripRepository extends JpaRepository<TripEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    Optional<TripEntity> findById(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TripEntity t WHERE t.id = :tripId")
    Optional<TripEntity> findByIdForUpdate(@Param("tripId") long tripId);

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_IdAndStatus(Long ownerId, TripStatus status);

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_IdAndTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
            Long ownerId,
            LocalDate startDate,
            TripStatus status
    );

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    Optional<TripEntity> findFirstByMotorcycle_Owner_IdAndTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
            Long ownerId,
            LocalDate startDate,
            TripStatus status
    );

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_IdAndTerrain(
            Long ownerId,
            TerrainType terrainType
    );

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_IdAndTripDate(
            Long ownerId,
            LocalDate tripDate
    );

    @EntityGraph(attributePaths = {"motorcycle", "motorcycle.owner"})
    List<TripEntity> findByMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
            Long ownerId,
            TripStatus status
    );

    boolean existsByMotorcycleId(Long motorcycleId);

}
