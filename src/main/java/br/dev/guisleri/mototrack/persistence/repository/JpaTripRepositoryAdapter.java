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
    public List<Trip> findAll() {
        return springDataTripRepository.findAll()
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByTerrain(TerrainType terrainType) {
        return springDataTripRepository.findByTerrain(terrainType)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByStatus(TripStatus status) {
        return springDataTripRepository.findByStatus(status)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public Map<TripStatus, Long> countByStatus() {
        Map<TripStatus, Long> tripCountsByStatus = new EnumMap<>(TripStatus.class);

        for (TripStatus status : TripStatus.values()) {
            tripCountsByStatus.put(status, 0L);
        }

        springDataTripRepository.countTripsGroupedByStatus()
                .forEach(statusCount ->
                        tripCountsByStatus.put(
                                statusCount.getStatus(),
                                statusCount.getTripCount()
                        )
                );

        return tripCountsByStatus;
    }

    @Override
    public Map<Motorcycle, Long> countByMotorcycle() {
        return springDataTripRepository.countTripsGroupedByMotorcycle()
                .stream()
                .collect(Collectors.toMap(
                        motorcycleCount -> motorcycleMapper.toDomain(
                                motorcycleCount.getMotorcycle()
                        ),
                        MotorcycleTripCountProjection::getTripCount
                ));
    }

    @Override
    public List<Trip> findByTripDate(LocalDate tripDate) {
        return springDataTripRepository.findByTripDate(tripDate)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findUpcomingFrom(LocalDate referenceDate) {
        return springDataTripRepository
                .findByTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
                        referenceDate,
                        TripStatus.PLANNED
                )
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public double sumDistanceKmByStatus(TripStatus status) {
        return springDataTripRepository.sumDistanceKmByStatus(status);
    }

    @Override
    public double sumDistanceKmByMotorcycleAndStatus(
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return springDataTripRepository.sumDistanceKmByMotorcycleIdAndStatus(
                motorcycle.getId(),
                status
        );
    }

    @Override
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips() {
        return springDataTripRepository.countTripsGroupedByMotorcycleAndStatus(
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

}
