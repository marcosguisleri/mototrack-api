package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
import br.dev.guisleri.mototrack.persistence.projection.MotorcycleTripCountProjection;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public Map<TripStatus, Long> countByOwnerIdAndStatus(Long ownerId) {
        Map<TripStatus, Long> tripCountsByStatus = new EnumMap<>(TripStatus.class);

        for (TripStatus status : TripStatus.values()) {
            tripCountsByStatus.put(status, 0L);
        }

        springDataTripRepository.countTripsGroupedByStatusForOwner(ownerId)
                .forEach(statusCount ->
                        tripCountsByStatus.put(
                                statusCount.getStatus(),
                                statusCount.getTripCount()
                        )
                );

        return tripCountsByStatus;
    }

    @Override
    public Map<Motorcycle, Long> countByOwnerIdAndMotorcycle(Long ownerId) {
        return springDataTripRepository.countTripsGroupedByMotorcycleForOwner(ownerId)
                .stream()
                .collect(Collectors.toMap(
                        motorcycleCount -> motorcycleMapper.toDomain(
                                motorcycleCount.getMotorcycle()
                        ),
                        MotorcycleTripCountProjection::getTripCount
                ));
    }

    @Override
    public double sumDistanceKmByOwnerIdAndStatus(
            Long ownerId,
            TripStatus status
    ) {
        return springDataTripRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                status
        );
    }

    @Override
    public double sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
            Long ownerId,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return springDataTripRepository.sumDistanceKmByMotorcycleIdAndOwnerIdAndStatus(
                motorcycle.getId(),
                ownerId,
                status
        );
    }

    @Override
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTripsByOwnerId(
            Long ownerId
    ) {
        return springDataTripRepository.countTripsGroupedByMotorcycleAndOwnerAndStatus(
                        ownerId,
                        TripStatus.COMPLETED
                )
                .stream()
                .max(Comparator.comparing(
                        MotorcycleTripCountProjection::getTripCount
                ))
                .map(MotorcycleTripCountProjection::getMotorcycle)
                .map(motorcycleMapper::toDomain);
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

}
