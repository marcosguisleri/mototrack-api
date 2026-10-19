package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;

import java.util.List;
import java.util.Optional;

public interface MotorcycleRepository {

    Motorcycle save(Motorcycle motorcycle);

    Optional<Motorcycle> findById(Long motorcycleId);

    Optional<Motorcycle> findByIdForUpdate(Long motorcycleId);

    void deleteById(Long motorcycleId);

    List<Motorcycle> findByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);

}
