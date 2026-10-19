package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataMotorcycleRepository extends JpaRepository<MotorcycleEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<MotorcycleEntity> findById(Long motorcycleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MotorcycleEntity m WHERE m.id = :motorcycleId")
    Optional<MotorcycleEntity> findByIdForUpdate(
            @Param("motorcycleId") Long motorcycleId
    );

    @EntityGraph(attributePaths = "owner")
    List<MotorcycleEntity> findByOwner_Id(Long ownerId);

    long countByOwner_Id(Long ownerId);

}
