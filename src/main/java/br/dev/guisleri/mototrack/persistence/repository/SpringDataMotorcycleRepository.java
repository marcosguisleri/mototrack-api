package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMotorcycleRepository extends JpaRepository<MotorcycleEntity, Long> {

    List<MotorcycleEntity> findByOwner_Id(Long ownerId);

}
