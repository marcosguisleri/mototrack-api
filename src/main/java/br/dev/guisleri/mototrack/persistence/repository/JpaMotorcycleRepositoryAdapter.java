package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.repository.MotorcycleRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaMotorcycleRepositoryAdapter implements MotorcycleRepository {

    private final SpringDataMotorcycleRepository springDataMotorcycleRepository;
    private final MotorcycleMapper motorcycleMapper;

    public JpaMotorcycleRepositoryAdapter(
            SpringDataMotorcycleRepository springDataMotorcycleRepository,
            MotorcycleMapper motorcycleMapper
    ) {
        this.springDataMotorcycleRepository = springDataMotorcycleRepository;
        this.motorcycleMapper = motorcycleMapper;
    }

    @Override
    @Transactional
    public Motorcycle save(Motorcycle motorcycle) {
        MotorcycleEntity savedMotorcycleEntity =
                springDataMotorcycleRepository.save(
                        motorcycleMapper.toEntity(motorcycle)
                );

        return motorcycleMapper.toDomain(savedMotorcycleEntity);
    }

    @Override
    public Optional<Motorcycle> findById(Long motorcycleId) {
        return springDataMotorcycleRepository
                .findById(motorcycleId)
                .map(motorcycleMapper::toDomain);
    }

    @Override
    public List<Motorcycle> findAll() {
        return springDataMotorcycleRepository.findAll()
                .stream()
                .map(motorcycleMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long motorcycleId) {
        springDataMotorcycleRepository.deleteById(motorcycleId);
    }

}
