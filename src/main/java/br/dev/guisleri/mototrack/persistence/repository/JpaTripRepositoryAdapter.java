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

    private final SpringDataTripRepository tripRepository;
    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final TripMapper tripMapper;
    private final MotorcycleMapper motorcycleMapper;

    public JpaTripRepositoryAdapter(
            SpringDataTripRepository tripRepository,
            SpringDataMotorcycleRepository motorcycleRepository,
            TripMapper tripMapper,
            MotorcycleMapper motorcycleMapper
    ) {
        this.tripRepository = tripRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.tripMapper = tripMapper;
        this.motorcycleMapper = motorcycleMapper;
    }

    @Override
    @Transactional(readOnly = false)
    public Trip save(Trip trip) {

        MotorcycleEntity motorcycleEntity =
                motorcycleRepository.save(
                        motorcycleMapper.toEntity(
                                trip.getMotorcycle()
                        )
                );

        TripEntity tripEntity =
                tripMapper.toEntity(
                        trip,
                        motorcycleEntity
                );

        TripEntity savedEntity =
                tripRepository.save(tripEntity);

        return tripMapper.toDomain(savedEntity);

    }

    @Override
    public Optional<Trip> findById(long id) {
        return tripRepository.findById(id)
                .map(tripMapper::toDomain);
    }

    @Override
    public List<Trip> findAll() {
        return tripRepository.findAll()
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByTerrain(TerrainType terrainType) {
        return tripRepository.findByTerrain(terrainType)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findByStatus(TripStatus status) {
        return tripRepository.findByStatus(status)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public Map<TripStatus, Long> countByStatus() {
        Map<TripStatus, Long> counts = new EnumMap<>(TripStatus.class);

        for (TripStatus status : TripStatus.values()) {
            counts.put(status, 0L);
        }

        tripRepository.countTripsGroupedByStatus()
                .forEach(projection ->
                        counts.put(
                                projection.getStatus(),
                                projection.getCount()
                        )
                );

        return counts;
    }

    @Override
    public Map<Motorcycle, Long> countByMotorcycle() {
        return tripRepository.countTripsGroupedByMotorcycle()
                .stream()
                .collect(Collectors.toMap(
                        projection -> motorcycleMapper.toDomain(
                                projection.getMotorcycle()
                        ),
                        MotorcycleTripCountProjection::getCount
                ));
    }

    @Override
    public List<Trip> findByTripDate(LocalDate date) {
        return tripRepository.findByTripDate(date)
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public List<Trip> findUpcomingFrom(LocalDate date) {
        return tripRepository
                .findByTripDateGreaterThanEqualAndStatusOrderByTripDateAsc(
                        date,
                        TripStatus.PLANNED
                )
                .stream()
                .map(tripMapper::toDomain)
                .toList();
    }

    @Override
    public double sumDistanceByStatus(TripStatus status) {
        return tripRepository.sumDistanceByStatus(status);
    }

    @Override
    public double sumDistanceByMotorcycleAndStatus(
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return tripRepository.sumDistanceByMotorcycleIdAndStatus(
                motorcycle.getId(),
                status
        );
    }

    @Override
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips() {
        return tripRepository.countTripsGroupedByMotorcycleAndStatus(
                        TripStatus.COMPLETED
                )
                .stream()
                .max(Comparator.comparing(
                        MotorcycleTripCountProjection::getCount
                ))
                .map(MotorcycleTripCountProjection::getMotorcycle)
                .map(motorcycleMapper::toDomain);
    }

}
