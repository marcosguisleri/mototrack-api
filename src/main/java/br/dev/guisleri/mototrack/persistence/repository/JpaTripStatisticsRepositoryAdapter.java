package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.projection.MotorcycleTripCountProjection;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaTripStatisticsRepositoryAdapter implements TripStatisticsRepository {

    private final SpringDataTripRepository springDataTripRepository;
    private final MotorcycleMapper motorcycleMapper;

    public JpaTripStatisticsRepositoryAdapter(
            SpringDataTripRepository springDataTripRepository,
            MotorcycleMapper motorcycleMapper
    ) {
        this.springDataTripRepository = springDataTripRepository;
        this.motorcycleMapper = motorcycleMapper;
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
    public double sumDistanceKmByOwnerIdAndStatus(Long ownerId, TripStatus status) {
        return springDataTripRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                status
        );
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
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTripsByOwnerId(Long ownerId) {
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
}
