package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
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
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
    }

    // -------------------------------------------------------------------------
    // Composição da resposta consolidada e estado vazio
    // -------------------------------------------------------------------------

    @Test
    void shouldBuildTripStatisticsResponse() {
        Trip longestTrip = completedTrip(
                10L,
                5_432.1,
                LocalDate.of(2025, 1, 15)
        );
        Trip firstCompletedTrip = completedTrip(
                9L,
                85.0,
                LocalDate.of(2024, 5, 10)
        );
        Trip lastCompletedTrip = completedTrip(
                11L,
                120.0,
                LocalDate.of(2026, 9, 20)
        );
        Map<TripStatus, Long> tripsByStatus = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 2L,
                TripStatus.COMPLETED, 3L
        );
        Map<TerrainType, TerrainStatistics> terrainStatistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(2L, 300.0)
        );
        when(tripStatisticsRepository.countTripsByStatus(OWNER_ID))
                .thenReturn(tripsByStatus);
        when(tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        )).thenReturn(244.50000000000003);
        when(tripStatisticsRepository
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(honda));
        when(tripStatisticsRepository.findLongestCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(longestTrip));
        when(tripStatisticsRepository.findFirstCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(firstCompletedTrip));
        when(tripStatisticsRepository.findLastCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(lastCompletedTrip));
        when(tripStatisticsRepository.findTerrainStatisticsByOwnerId(OWNER_ID))
                .thenReturn(terrainStatistics);
        when(tripStatisticsRepository.findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        )).thenReturn(List.of(
                new MonthlyTripStatistics(2026, 9, 1L, 120.0)
        ));

        TripStatisticsResponseDTO result =
                statisticsService.getStatistics(OWNER_ID);

        assertEquals(3L, result.totalCompletedTrips());
        assertEquals(244.5, result.totalCompletedDistance(), 0.001);
        assertEquals(81.5, result.averageCompletedDistanceKm(), 0.001);
        assertEquals(honda.getId(), result.mostUsedMotorcycle().id());
        assertEquals(longestTrip.getId(), result.longestTrip().id());
        assertEquals(firstCompletedTrip.getId(), result.firstCompletedTrip().id());
        assertEquals(lastCompletedTrip.getId(), result.lastCompletedTrip().id());
        assertSame(tripsByStatus, result.tripsByStatus());
        assertSame(terrainStatistics, result.terrainStatistics());
        assertEquals(12, result.monthlyStatistics().size());
    }

    @Test
    void shouldBuildEmptyTripStatisticsResponse() {
        when(tripStatisticsRepository.countTripsByStatus(OWNER_ID))
                .thenReturn(Map.of());
        when(tripStatisticsRepository.sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        )).thenReturn(0.0);
        when(tripStatisticsRepository
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsRepository.findLongestCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsRepository.findFirstCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsRepository.findLastCompletedTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsRepository.findTerrainStatisticsByOwnerId(OWNER_ID))
                .thenReturn(Map.of());
        when(tripStatisticsRepository.findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        )).thenReturn(List.of());

        TripStatisticsResponseDTO result =
                statisticsService.getStatistics(OWNER_ID);

        assertEquals(0L, result.totalCompletedTrips());
        assertEquals(0.0, result.totalCompletedDistance(), 0.001);
        assertNull(result.averageCompletedDistanceKm());
        assertNull(result.mostUsedMotorcycle());
        assertNull(result.longestTrip());
        assertNull(result.firstCompletedTrip());
        assertNull(result.lastCompletedTrip());
        assertTrue(result.tripsByStatus().isEmpty());
        assertTrue(result.terrainStatistics().isEmpty());
        assertEquals(12, result.monthlyStatistics().size());
    }

    // -------------------------------------------------------------------------
    // Contagens por status e total de viagens concluídas
    // -------------------------------------------------------------------------

    @Test
    void shouldCountTripsByStatus() {
        Map<TripStatus, Long> counts = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 1L,
                TripStatus.COMPLETED, 1L
        );
        when(tripStatisticsRepository.countTripsByStatus(OWNER_ID)).thenReturn(counts);

        Map<TripStatus, Long> result = statisticsService.countTripsByStatus(OWNER_ID);

        assertSame(counts, result);
        verify(tripStatisticsRepository).countTripsByStatus(OWNER_ID);
    }

    @Test
    void shouldCountCompletedTrips() {
        when(tripStatisticsRepository.countTripsByStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.COMPLETED, 1L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(1L, result);
        verify(tripStatisticsRepository).countTripsByStatus(OWNER_ID);
    }

    @Test
    void shouldReturnZeroWhenThereAreNoCompletedTrips() {
        when(tripStatisticsRepository.countTripsByStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.PLANNED, 2L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(0L, result);
        verify(tripStatisticsRepository).countTripsByStatus(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Distância total concluída
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Estatísticas por terreno e por motocicleta
    // -------------------------------------------------------------------------

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
    void shouldFindTerrainStatisticsByMotorcycle() {
        Map<TerrainType, TerrainStatistics> statistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(2L, 300.0),
                TerrainType.MIXED,
                new TerrainStatistics(1L, 300.0),
                TerrainType.OFF_ROAD,
                new TerrainStatistics(0L, 0.0)
        );
        when(tripStatisticsRepository
                .findTerrainStatisticsByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                ))
                .thenReturn(statistics);

        Map<TerrainType, TerrainStatistics> result = statisticsService
                .findTerrainStatisticsByMotorcycle(OWNER_ID, honda);

        assertSame(statistics, result);
        verify(tripStatisticsRepository)
                .findTerrainStatisticsByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                );
    }

    // -------------------------------------------------------------------------
    // Série mensal dos últimos doze meses
    // -------------------------------------------------------------------------

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

        assertEquals(12, result.size());
        assertEquals(
                new MonthlyTripStatistics(2025, 10, 2L, 250.0),
                result.get(0)
        );
        assertEquals(
                new MonthlyTripStatistics(2025, 11, 1L, 300.0),
                result.get(1)
        );
        assertEquals(
                new MonthlyTripStatistics(2025, 12, 0L, 0.0),
                result.get(2)
        );
        assertEquals(
                new MonthlyTripStatistics(2026, 9, 0L, 0.0),
                result.get(11)
        );
        verify(tripStatisticsRepository).findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        );
    }

    // -------------------------------------------------------------------------
    // Métricas anuais e agregados por motocicleta
    // -------------------------------------------------------------------------

    @Test
    void shouldCalculateCompletedDistanceForCurrentYear() {
        when(tripStatisticsRepository.findMonthlyStatisticsByOwnerIdFromDate(
                OWNER_ID,
                LocalDate.of(2025, 10, 1)
        )).thenReturn(List.of(
                new MonthlyTripStatistics(2025, 12, 1L, 500.0),
                new MonthlyTripStatistics(2026, 1, 2L, 250.0),
                new MonthlyTripStatistics(2026, 9, 1L, 125.5)
        ));

        double result = statisticsService
                .calculateCompletedDistanceKmForCurrentYear(OWNER_ID);

        assertEquals(375.5, result, 0.001);
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
    void shouldCountCompletedTripsByMotorcycle() {
        when(tripStatisticsRepository.countByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        )).thenReturn(2L);

        long result = statisticsService
                .countCompletedTripsByMotorcycle(OWNER_ID, honda);

        assertEquals(2L, result);
        verify(tripStatisticsRepository).countByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        );
    }

    // -------------------------------------------------------------------------
    // Histórico de viagens por motocicleta
    // -------------------------------------------------------------------------

    @Test
    void shouldFindLongestCompletedTripByMotorcycle() {
        Optional<Trip> expected = Optional.of(Trip.restore(
                1L,
                "Florianopolis",
                "Urubici",
                250.0,
                TerrainType.MIXED,
                LocalDate.of(2026, 1, 10),
                honda,
                TripStatus.COMPLETED
        ));
        when(tripStatisticsRepository
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                ))
                .thenReturn(expected);

        Optional<Trip> result = statisticsService
                .findLongestCompletedTripByMotorcycle(OWNER_ID, honda);

        assertSame(expected, result);
        verify(tripStatisticsRepository)
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                );
    }

    @Test
    void shouldFindLastCompletedTripByMotorcycle() {
        Optional<Trip> expected = Optional.of(Trip.restore(
                1L,
                "Florianopolis",
                "Urubici",
                250.0,
                TerrainType.MIXED,
                LocalDate.of(2026, 1, 10),
                honda,
                TripStatus.COMPLETED
        ));
        when(tripStatisticsRepository
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                ))
                .thenReturn(expected);

        Optional<Trip> result = statisticsService
                .findLastCompletedTripByMotorcycle(OWNER_ID, honda);

        assertSame(expected, result);
        verify(tripStatisticsRepository)
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        OWNER_ID,
                        honda
                );
    }

    // -------------------------------------------------------------------------
    // Rankings e histórico geral de viagens concluídas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Helpers de domínio
    // -------------------------------------------------------------------------

    private Trip completedTrip(
            Long id,
            double distanceKm,
            LocalDate tripDate
    ) {
        return Trip.restore(
                id,
                "Araras",
                "Destination",
                distanceKm,
                TerrainType.MIXED,
                tripDate,
                honda,
                TripStatus.COMPLETED
        );
    }
}
