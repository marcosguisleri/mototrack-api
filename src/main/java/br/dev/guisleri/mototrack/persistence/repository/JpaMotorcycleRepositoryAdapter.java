package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
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
    private final SpringDataUserRepository springDataUserRepository;
    private final MotorcycleMapper motorcycleMapper;

    public JpaMotorcycleRepositoryAdapter(
            SpringDataMotorcycleRepository springDataMotorcycleRepository,
            SpringDataUserRepository springDataUserRepository,
            MotorcycleMapper motorcycleMapper
    ) {
        this.springDataMotorcycleRepository = springDataMotorcycleRepository;
        this.springDataUserRepository = springDataUserRepository;
        this.motorcycleMapper = motorcycleMapper;
    }

    @Override
    @Transactional
    public Motorcycle save(Motorcycle motorcycle) {

        UserEntity userEntity = springDataUserRepository
                .findById(motorcycle.getOwner().getId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id %d não encontrado"
                                .formatted(motorcycle.getOwner().getId())
                ));

        MotorcycleEntity motorcycleEntity =
                motorcycleMapper.toEntity(
                        motorcycle,
                        userEntity
                );

        MotorcycleEntity savedMotorcycleEntity =
                springDataMotorcycleRepository.save(motorcycleEntity);

        return motorcycleMapper.toDomain(savedMotorcycleEntity);
    }

    @Override
    public Optional<Motorcycle> findById(Long motorcycleId) {
        return springDataMotorcycleRepository
                .findById(motorcycleId)
                .map(motorcycleMapper::toDomain);
    }

    @Override
    public List<Motorcycle> findByOwnerId(Long ownerId) {
        return springDataMotorcycleRepository
                .findByOwner_Id(ownerId)
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
