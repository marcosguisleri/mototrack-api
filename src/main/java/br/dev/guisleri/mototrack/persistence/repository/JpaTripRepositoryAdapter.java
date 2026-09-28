package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaTripRepositoryAdapter implements TripRepository {

    private final SpringDataTripRepository springDataTripRepository;
    private final SpringDataMotorcycleRepository springDataMotorcycleRepository;
    private final TripMapper tripMapper;
    private final MotorcycleMapper motorcycleMapper;

    public JpaTripRepositoryAdapter(
            SpringDataTripRepository springDataTripRepository,
            SpringDataMotorcycleRepository springDataMotorcycleRepository,
            TripMapper tripMapper,
            MotorcycleMapper motorcycleMapper
    ) {
        this.springDataTripRepository = springDataTripRepository;
        this.springDataMotorcycleRepository = springDataMotorcycleRepository;
        this.tripMapper = tripMapper;
        this.motorcycleMapper = motorcycleMapper;
    }

    @Override
    @Transactional(readOnly = false)
    public Trip save(Trip trip) {

        MotorcycleEntity motorcycleEntity =
                springDataMotorcycleRepository.findById(
                                trip.getMotorcycle().getId()
                        )
                        .orElseThrow();

        TripEntity tripEntity =
                tripMapper.toEntity(
                        trip,
                        motorcycleEntity
                );

        TripEntity savedTripEntity =
                springDataTripRepository.save(tripEntity);

        return tripMapper.toDomain(savedTripEntity);

    }

    @Override
    public Optional<Trip> findById(long tripId) {
        return springDataTripRepository.findById(tripId)
                .map(tripMapper::toDomain);
    }

    @Override
    public boolean existsByMotorcycleId(Long motorcycleId) {
        return springDataTripRepository.existsByMotorcycleId(motorcycleId);
    }

    @Override
    @Transactional
    public void deleteById(Long tripId) {
        springDataTripRepository.deleteById(tripId);
    }

    @Override
    public List<Trip> findByOwnerId(Long ownerId) {
        return springDataTripRepository.findByMotorcycle_Owner_Id(ownerId)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByOwnerIdAndStatus(Long ownerId, TripStatus status) {
        return springDataTripRepository.findByMotorcycle_Owner_IdAndStatus(
                        ownerId,
                        status
                ).stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findUpcomingFromByOwnerId(Long ownerId, LocalDate startDate) {
        return springDataTripRepository.findByMotorcycle_Owner_IdAndTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
                        ownerId,
                        startDate,
                        TripStatus.PLANNED
                ).stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByOwnerIdAndTerrain(Long ownerId, TerrainType terrainType) {
        return springDataTripRepository.findByMotorcycle_Owner_IdAndTerrain(
                        ownerId,
                        terrainType
                ).stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByOwnerIdAndTripDate(
            Long ownerId,
            LocalDate tripDate
    ) {
        return springDataTripRepository.findByMotorcycle_Owner_IdAndTripDate(
                ownerId,
                tripDate
        ).stream()
            .map(tripMapper::toDomain)
            .toList();
    }

    @Override
    public List<Trip> findCompletedByOwnerIdOrderByTripDateDesc(Long ownerId) {
        return springDataTripRepository
                .findByMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
                        ownerId,
                        TripStatus.COMPLETED
                )
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

}
