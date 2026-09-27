package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripStatisticsServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Mock
    private TripStatisticsRepository tripStatisticsRepository;

    private TripStatisticsService statisticsService;
    private Motorcycle honda;
    private Motorcycle yamaha;

    @BeforeEach
    void setUp() {
        statisticsService = new TripStatisticsService(
                tripStatisticsRepository,
                FIXED_CLOCK
        );
        User owner = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        honda = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, owner
        );
        yamaha = Motorcycle.restore(
                2L, "Yamaha", "Tenere 700", "Blue", 2024, 689, owner
        );
    }

    @Test
    void shouldCountTripsByStatus() {
        Map<TripStatus, Long> counts = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 1L,
                TripStatus.COMPLETED, 1L
        );
        when(tripStatisticsRepository.countByOwnerIdAndStatus(OWNER_ID)).thenReturn(counts);

        Map<TripStatus, Long> result = statisticsService.countTripsByStatus(OWNER_ID);

        assertSame(counts, result);
        verify(tripStatisticsRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldCountCompletedTrips() {
        when(tripStatisticsRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.COMPLETED, 1L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(1L, result);
        verify(tripStatisticsRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldReturnZeroWhenThereAreNoCompletedTrips() {
        when(tripStatisticsRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.PLANNED, 2L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(0L, result);
        verify(tripStatisticsRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldCalculateTotalCompletedDistanceKm() {
        when(tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        ))
                .thenReturn(250.0);

        double result = statisticsService.calculateTotalCompletedDistanceKm(OWNER_ID);

        assertEquals(250.0, result, 0.001);
        verify(tripStatisticsRepository).sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldCalculateAverageCompletedDistanceKm() {
        when(tripStatisticsRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.COMPLETED, 2L));
        when(tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        )).thenReturn(250.0);

        Double result = statisticsService.calculateAverageCompletedDistanceKm(OWNER_ID);

        assertEquals(125.0, result, 0.001);
        verify(tripStatisticsRepository).countByOwnerIdAndStatus(OWNER_ID);
        verify(tripStatisticsRepository).sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldReturnNullAverageWhenThereAreNoCompletedTrips() {
        when(tripStatisticsRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of());

        Double result = statisticsService.calculateAverageCompletedDistanceKm(OWNER_ID);

        assertNull(result);
        verify(tripStatisticsRepository).countByOwnerIdAndStatus(OWNER_ID);
        verify(tripStatisticsRepository, never()).sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        Map<Motorcycle, Long> counts = Map.of(
                honda, 2L,
                yamaha, 1L
        );
        when(tripStatisticsRepository.countByOwnerIdAndMotorcycle(OWNER_ID)).thenReturn(counts);

        Map<Motorcycle, Long> result = statisticsService.countTripsByMotorcycle(OWNER_ID);

        assertSame(counts, result);
        verify(tripStatisticsRepository).countByOwnerIdAndMotorcycle(OWNER_ID);
    }

    @Test
    void shouldFindTerrainStatistics() {
        Map<TerrainType, TerrainStatistics> statistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(3L, 500.0),
                TerrainType.MIXED,
                new TerrainStatistics(2L, 250.0)
        );
        when(tripStatisticsRepository.findTerrainStatisticsByOwnerId(OWNER_ID))
                .thenReturn(statistics);

        Map<TerrainType, TerrainStatistics> result =
                statisticsService.findTerrainStatistics(OWNER_ID);

        assertSame(statistics, result);
        verify(tripStatisticsRepository).findTerrainStatisticsByOwnerId(OWNER_ID);
    }

    @Test
    void shouldFindMonthlyStatisticsFromTheBeginningOfTheLast12Months() {
        List<MonthlyTripStatistics> statistics = List.of(
                new MonthlyTripStatistics(2025, 10, 2L, 250.0),
                new MonthlyTripStatistics(2025, 11, 1L, 300.0)
        );
        when(tripStatisticsRepository.findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        )).thenReturn(statistics);

        List<MonthlyTripStatistics> result =
                statisticsService.findMonthlyStatisticsLast12Months(OWNER_ID);

        assertSame(statistics, result);
        verify(tripStatisticsRepository).findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        );
    }

    @Test
    void shouldCalculateCompletedDistanceKmByMotorcycle() {
        when(tripStatisticsRepository.sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        )).thenReturn(100.0);

        double result = statisticsService
                .calculateCompletedDistanceKmByMotorcycle(OWNER_ID, honda);

        assertEquals(100.0, result, 0.001);
        verify(tripStatisticsRepository).sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldFindMostUsedMotorcycle() {
        when(tripStatisticsRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(honda));

        Optional<Motorcycle> result =
                statisticsService.findMostUsedMotorcycleInCompletedTrips(OWNER_ID);

        assertEquals(Optional.of(honda), result);
        verify(tripStatisticsRepository)
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoCompletedTrips() {
        when(tripStatisticsRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        Optional<Motorcycle> result =
                statisticsService.findMostUsedMotorcycleInCompletedTrips(OWNER_ID);

        assertTrue(result.isEmpty());
        verify(tripStatisticsRepository)
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID);
    }

    @Test
    void shouldFindLongestCompletedTrip() {
        Trip longestTrip = Trip.restore(
                1L,
                "Florianopolis",
                "Urubici",
                250.0,
                TerrainType.MIXED,
                LocalDate.of(2026, 1, 10),
                honda,
                TripStatus.COMPLETED
        );
        when(tripStatisticsRepository.findLongestCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(longestTrip));

        Optional<Trip> result = statisticsService.findLongestCompletedTrip(OWNER_ID);

        assertSame(longestTrip, result.orElseThrow());
        verify(tripStatisticsRepository).findLongestCompletedTripByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoLongestCompletedTrip() {
        when(tripStatisticsRepository.findLongestCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        Optional<Trip> result = statisticsService.findLongestCompletedTrip(OWNER_ID);

        assertTrue(result.isEmpty());
        verify(tripStatisticsRepository).findLongestCompletedTripByOwnerId(OWNER_ID);
    }

    @Test
    void shouldFindLastCompletedTrip() {
        Trip lastTrip = Trip.restore(
                2L,
                "Florianopolis",
                "Blumenau",
                140.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 2, 15),
                honda,
                TripStatus.COMPLETED
        );
        when(tripStatisticsRepository.findLastCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(lastTrip));

        Optional<Trip> result = statisticsService.findLastCompletedTrip(OWNER_ID);

        assertSame(lastTrip, result.orElseThrow());
        verify(tripStatisticsRepository).findLastCompletedTripByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoLastCompletedTrip() {
        when(tripStatisticsRepository.findLastCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        Optional<Trip> result = statisticsService.findLastCompletedTrip(OWNER_ID);

        assertTrue(result.isEmpty());
        verify(tripStatisticsRepository).findLastCompletedTripByOwnerId(OWNER_ID);
    }

    @Test
    void shouldFindFirstCompletedTrip() {
        Trip firstTrip = Trip.restore(
                3L,
                "Araras",
                "Rio Claro",
                85.0,
                TerrainType.ASPHALT,
                LocalDate.of(2024, 5, 10),
                honda,
                TripStatus.COMPLETED
        );
        when(tripStatisticsRepository.findFirstCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(firstTrip));

        Optional<Trip> result = statisticsService.findFirstCompletedTrip(OWNER_ID);

        assertSame(firstTrip, result.orElseThrow());
        verify(tripStatisticsRepository).findFirstCompletedTripByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoFirstCompletedTrip() {
        when(tripStatisticsRepository.findFirstCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        Optional<Trip> result = statisticsService.findFirstCompletedTrip(OWNER_ID);

        assertTrue(result.isEmpty());
        verify(tripStatisticsRepository).findFirstCompletedTripByOwnerId(OWNER_ID);
    }
}
