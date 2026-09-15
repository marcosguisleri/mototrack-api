package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMotorcycleRepository extends JpaRepository<MotorcycleEntity, Long> {
}
