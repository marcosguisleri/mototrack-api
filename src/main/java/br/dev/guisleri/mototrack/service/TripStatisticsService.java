package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TripStatisticsService {

    private final TripStatisticsRepository tripStatisticsRepository;
    private final Clock clock;

    public TripStatisticsService(
            TripStatisticsRepository tripStatisticsRepository,
            Clock clock
    ) {
        this.tripStatisticsRepository = tripStatisticsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TripStatisticsResponseDTO getStatistics(Long ownerId) {
        Map<TripStatus, Long> tripsByStatus = countTripsByStatus(ownerId);
        long totalCompletedTrips =
                tripsByStatus.getOrDefault(TripStatus.COMPLETED, 0L);
        double totalCompletedDistanceKm =
                calculateTotalCompletedDistanceKm(ownerId);
        Double averageCompletedDistance =
                totalCompletedTrips == 0
                        ? null
                        : totalCompletedDistanceKm / totalCompletedTrips;
        MotorcycleResponseDTO mostUsedMotorcycle =
                findMostUsedMotorcycleInCompletedTrips(ownerId)
                        .map(MotorcycleResponseDTO::from)
                        .orElse(null);
        TripSummaryResponseDTO longestTrip =
                findLongestCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);
        TripSummaryResponseDTO firstCompletedTrip =
                findFirstCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);
        TripSummaryResponseDTO lastCompletedTrip =
                findLastCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        return new TripStatisticsResponseDTO(
                totalCompletedTrips,
                roundToOneDecimal(totalCompletedDistanceKm),
                averageCompletedDistance,
                mostUsedMotorcycle,
                longestTrip,
                firstCompletedTrip,
                lastCompletedTrip,
                tripsByStatus,
                findTerrainStatistics(ownerId),
                findMonthlyStatisticsLast12Months(ownerId)
        );
    }

    // Overall statistics

    public long countCompletedTrips(Long ownerId) {
        return tripStatisticsRepository.countTripsByStatus(ownerId)
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistanceKm(Long ownerId) {
        return tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                TripStatus.COMPLETED
        );
    }

    public Map<TripStatus, Long> countTripsByStatus(Long ownerId) {
        return tripStatisticsRepository.countTripsByStatus(ownerId);
    }

    // Trip highlights

    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips(
            Long ownerId
    ) {
        return tripStatisticsRepository
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(ownerId);
    }

    public Optional<Trip> findLongestCompletedTrip(Long ownerId) {
        return tripStatisticsRepository
                .findLongestCompletedTripByOwnerId(ownerId);
    }

    public Optional<Trip> findLastCompletedTrip(Long ownerId) {
        return tripStatisticsRepository
                .findLastCompletedTripByOwnerId(ownerId);
    }

    public Optional<Trip> findFirstCompletedTrip(Long ownerId) {
        return tripStatisticsRepository
                .findFirstCompletedTripByOwnerId(ownerId);
    }

    // Distributions

    public Map<TerrainType, TerrainStatistics> findTerrainStatistics(
            Long ownerId
    ) {
        return tripStatisticsRepository.findTerrainStatisticsByOwnerId(ownerId);
    }

    public List<MonthlyTripStatistics> findMonthlyStatisticsLast12Months(
            Long ownerId
    ) {
        LocalDate currentMonth =
                LocalDate.now(clock).withDayOfMonth(1);

        LocalDate startDate =
                currentMonth.minusMonths(11);

        List<MonthlyTripStatistics> monthlyStatistics =
                tripStatisticsRepository.findMonthlyStatisticsByOwnerIdFromDate(
                        ownerId,
                        startDate
                );

        Map<YearMonth, MonthlyTripStatistics> statisticsByMonth =
                new HashMap<>(12);

        for (MonthlyTripStatistics statistic : monthlyStatistics) {
            statisticsByMonth.put(
                    YearMonth.of(
                            statistic.year(),
                            statistic.month()
                    ),
                    statistic
            );
        }

        List<MonthlyTripStatistics> result =
                new ArrayList<>(12);

        YearMonth startMonth = YearMonth.from(startDate);

        for (int i = 0; i < 12; i++) {
            YearMonth month = startMonth.plusMonths(i);

            MonthlyTripStatistics statistic =
                    statisticsByMonth.getOrDefault(
                            month,
                            new MonthlyTripStatistics(
                                    month.getYear(),
                                    month.getMonthValue(),
                                    0L,
                                    0.0
                            )
                    );

            result.add(statistic);
        }

        return result;
    }

    public double calculateCompletedDistanceKmForCurrentYear(Long ownerId) {
        int currentYear = LocalDate.now(clock).getYear();

        return findMonthlyStatisticsLast12Months(ownerId)
                .stream()
                .filter(statistic -> statistic.year() == currentYear)
                .mapToDouble(MonthlyTripStatistics::totalDistanceKm)
                .sum();
    }

    // Motorcycle statistics

    public double calculateCompletedDistanceKmByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository
                .sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                        ownerId,
                        motorcycle,
                        TripStatus.COMPLETED
                );
    }

    public long countCompletedTripsByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository
                .countByOwnerIdAndMotorcycleAndStatus(
                        ownerId,
                        motorcycle,
                        TripStatus.COMPLETED
                );
    }

    public Optional<Trip> findLongestCompletedTripByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        ownerId,
                        motorcycle
                );
    }

    public Optional<Trip> findLastCompletedTripByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        ownerId,
                        motorcycle
                );
    }

    public Map<TerrainType, TerrainStatistics> findTerrainStatisticsByMotorcycle(
            Long ownerId,
            Motorcycle motorcycle
    ) {
        return tripStatisticsRepository.findTerrainStatisticsByOwnerIdAndMotorcycle(
                ownerId,
                motorcycle
        );
    }

    private double roundToOneDecimal(double distanceKm) {
        return Math.round(distanceKm * 10.0) / 10.0;
    }
}
