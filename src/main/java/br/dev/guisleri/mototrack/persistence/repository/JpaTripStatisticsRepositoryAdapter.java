package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
import br.dev.guisleri.mototrack.persistence.projection.MotorcycleTripCountProjection;
import br.dev.guisleri.mototrack.persistence.projection.TerrainTripStatisticsProjection;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class JpaTripStatisticsRepositoryAdapter
        implements TripStatisticsRepository {

    private final SpringDataTripStatisticsRepository springDataTripStatisticsRepository;
    private final MotorcycleMapper motorcycleMapper;
    private final TripMapper tripMapper;

    public JpaTripStatisticsRepositoryAdapter(
            SpringDataTripStatisticsRepository springDataTripStatisticsRepository,
            MotorcycleMapper motorcycleMapper,
            TripMapper tripMapper
    ) {
        this.springDataTripStatisticsRepository = springDataTripStatisticsRepository;
        this.motorcycleMapper = motorcycleMapper;
        this.tripMapper = tripMapper;
    }

    // Agregações gerais

    @Override
    public Map<TripStatus, Long> countByOwnerIdAndStatus(Long ownerId) {
        Map<TripStatus, Long> tripCountsByStatus = new EnumMap<>(TripStatus.class);

        for (TripStatus status : TripStatus.values()) {
            tripCountsByStatus.put(status, 0L);
        }

        springDataTripStatisticsRepository.countTripsGroupedByStatusForOwner(ownerId)
                .forEach(statusCount ->
                        tripCountsByStatus.put(
                                statusCount.getStatus(),
                                statusCount.getTripCount()
                        )
                );

        return tripCountsByStatus;
    }

    @Override
    public double sumDistanceKmByOwnerIdAndStatus(
            Long ownerId,
            TripStatus status
    ) {
        return springDataTripStatisticsRepository
                .sumDistanceKmByOwnerIdAndStatus(
                        ownerId,
                        status
                );
    }

    @Override
    public Map<TerrainType, TerrainStatistics> findTerrainStatisticsByOwnerId(
            Long ownerId
    ) {
        Map<TerrainType, TerrainStatistics> terrainStatisticsByTerrain =
                new EnumMap<>(TerrainType.class);

        for (TerrainType terrain : TerrainType.values()) {
            terrainStatisticsByTerrain.put(
                    terrain,
                    new TerrainStatistics(0L, 0.0)
            );
        }

        List<TerrainTripStatisticsProjection> terrainStatistics =
                springDataTripStatisticsRepository
                        .findTerrainStatisticsByOwnerIdAndStatus(
                                ownerId,
                                TripStatus.COMPLETED
                        );

        for (TerrainTripStatisticsProjection statistic : terrainStatistics) {
            terrainStatisticsByTerrain.put(
                    statistic.getTerrain(),
                    new TerrainStatistics(
                            statistic.getTripCount(),
                            statistic.getTotalDistanceKm()
                    )
            );
        }

        return terrainStatisticsByTerrain;
    }

    @Override
    public List<MonthlyTripStatistics> findMonthlyStatisticsByOwnerIdFromDate(
            Long ownerId,
            LocalDate startDate
    ) {
        return springDataTripStatisticsRepository
                .findMonthlyStatisticsByOwnerIdAndStatusFromDate(
                        ownerId,
                        TripStatus.COMPLETED,
                        startDate
                )
                .stream()
                .map(statistic -> new MonthlyTripStatistics(
                        statistic.getYear(),
                        statistic.getMonth(),
                        statistic.getTripCount(),
                        statistic.getTotalDistanceKm()
                ))
                .toList();
    }

    // Destaques de viagens

    @Override
    public Optional<Trip> findLongestCompletedTripByOwnerId(Long ownerId) {
        return springDataTripStatisticsRepository
                .findFirstByMotorcycle_Owner_IdAndStatusOrderByDistanceKmDescTripDateDescIdDesc(
                        ownerId,
                        TripStatus.COMPLETED
                )
                .map(tripMapper::toDomain);
    }

    @Override
    public Optional<Trip> findLastCompletedTripByOwnerId(Long ownerId) {
        return springDataTripStatisticsRepository
                .findFirstByMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
                        ownerId,
                        TripStatus.COMPLETED
                )
                .map(tripMapper::toDomain);
    }

    @Override
    public Optional<Trip> findFirstCompletedTripByOwnerId(Long ownerId) {
        return springDataTripStatisticsRepository
                .findFirstByMotorcycle_Owner_IdAndStatusOrderByTripDateAscIdAsc(
                        ownerId,
                        TripStatus.COMPLETED
                )
                .map(tripMapper::toDomain);
    }

    // Estatísticas por motocicleta

    @Override
    public Map<Motorcycle, Long> countByOwnerIdAndMotorcycle(Long ownerId) {
        return springDataTripStatisticsRepository
                .countTripsGroupedByMotorcycleForOwner(ownerId)
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
        return springDataTripStatisticsRepository
                .sumDistanceKmByMotorcycleIdAndOwnerIdAndStatus(
                        motorcycle.getId(),
                        ownerId,
                        status
                );
    }

    @Override
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTripsByOwnerId(
            Long ownerId
    ) {
        return springDataTripStatisticsRepository
                .countTripsGroupedByMotorcycleAndOwnerAndStatus(
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
    public Long countByOwnerIdAndMotorcycleAndStatus(
            Long ownerId,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return springDataTripStatisticsRepository
                .countByMotorcycle_IdAndMotorcycle_Owner_IdAndStatus(
                        motorcycle.getId(),
                        ownerId,
                        status
                );
    }

    @Override
    public Optional<Trip> findLongestCompletedTripByOwnerIdAndMotorcycle(Long ownerId, Motorcycle motorcycle) {
        return springDataTripStatisticsRepository
                .findFirstByMotorcycle_IdAndMotorcycle_Owner_IdAndStatusOrderByDistanceKmDescTripDateDescIdDesc(
                        motorcycle.getId(),
                        ownerId,
                        TripStatus.COMPLETED
                )
                .map(tripMapper::toDomain);
    }

    @Override
    public Optional<Trip> findLastCompletedTripByOwnerIdAndMotorcycle(Long ownerId, Motorcycle motorcycle) {
        return springDataTripStatisticsRepository
                .findFirstByMotorcycle_IdAndMotorcycle_Owner_IdAndStatusOrderByTripDateDescIdDesc(
                        motorcycle.getId(),
                        ownerId,
                        TripStatus.COMPLETED
                )
                .map(tripMapper::toDomain);
    }

    @Override
    public Map<TerrainType, TerrainStatistics> findTerrainStatisticsByOwnerIdAndMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        Map<TerrainType, TerrainStatistics> statistics =
                new EnumMap<>(TerrainType.class);

        for (TerrainType terrain : TerrainType.values()) {
            statistics.put(
                    terrain,
                    new TerrainStatistics(0L, 0.0)
            );
        }

        springDataTripStatisticsRepository
                .findTerrainStatisticsByMotorcycleIdAndOwnerIdAndStatus(
                        motorcycle.getId(),
                        ownerId,
                        TripStatus.COMPLETED
                )
                .forEach(result -> statistics.put(
                        result.getTerrain(),
                        new TerrainStatistics(
                                result.getTripCount(),
                                result.getTotalDistanceKm()
                        )
                ));

        return statistics;
    }

}
