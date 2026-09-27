package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.*;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class TripStatisticsService {

    private final TripStatisticsRepository tripStatisticsRepository;
    private final Clock clock;

    public TripStatisticsService(
            TripStatisticsRepository tripStatisticsRepository, Clock clock
    ) {
        this.tripStatisticsRepository = tripStatisticsRepository;
        this.clock = clock;
    }

    // Resumo geral

    public Long countCompletedTrips(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndStatus(ownerId)
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public Double calculateTotalCompletedDistanceKm(Long ownerId) {
        return tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                ownerId,
                TripStatus.COMPLETED
        );
    }

    public Double calculateAverageCompletedDistanceKm(Long ownerId) {
        long completedTrips = countCompletedTrips(ownerId);

        // Sem viagens concluídas, não existe uma média.
        if (completedTrips == 0) {
            return null;
        }

        double totalKm = calculateTotalCompletedDistanceKm(ownerId);

        return totalKm / completedTrips;
    }

    public Map<TripStatus, Long> countTripsByStatus(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndStatus(ownerId);
    }

    // Destaques

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

    // Distribuições

    public Map<Motorcycle, Long> countTripsByMotorcycle(Long ownerId) {
        return tripStatisticsRepository.countByOwnerIdAndMotorcycle(ownerId);
    }

    public Map<TerrainType, TerrainStatistics> findTerrainStatistics(Long ownerId) {
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
                new HashMap<>();

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
                new ArrayList<>();

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

    // Estatísticas por motocicleta

    public Double calculateCompletedDistanceKmByMotorcycle(
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
}