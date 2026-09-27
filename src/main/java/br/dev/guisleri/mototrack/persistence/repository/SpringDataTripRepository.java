package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataTripRepository extends JpaRepository<TripEntity, Long> {

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
