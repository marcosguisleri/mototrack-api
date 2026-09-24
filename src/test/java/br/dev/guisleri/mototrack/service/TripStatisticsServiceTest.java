package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripStatisticsServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private TripRepository tripRepository;

    private TripStatisticsService statisticsService;
    private Motorcycle honda;
    private Motorcycle yamaha;

    @BeforeEach
    void setUp() {
        statisticsService = new TripStatisticsService(tripRepository);
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
        when(tripRepository.countByOwnerIdAndStatus(OWNER_ID)).thenReturn(counts);

        Map<TripStatus, Long> result = statisticsService.countTripsByStatus(OWNER_ID);

        assertSame(counts, result);
        verify(tripRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldCountCompletedTrips() {
        when(tripRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.COMPLETED, 1L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(1L, result);
        verify(tripRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldReturnZeroWhenThereAreNoCompletedTrips() {
        when(tripRepository.countByOwnerIdAndStatus(OWNER_ID))
                .thenReturn(Map.of(TripStatus.PLANNED, 2L));

        long result = statisticsService.countCompletedTrips(OWNER_ID);

        assertEquals(0L, result);
        verify(tripRepository).countByOwnerIdAndStatus(OWNER_ID);
    }

    @Test
    void shouldCalculateTotalCompletedDistanceKm() {
        when(tripRepository.sumDistanceKmByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.COMPLETED
        ))
                .thenReturn(250.0);

        double result = statisticsService.calculateTotalCompletedDistanceKm(OWNER_ID);

        assertEquals(250.0, result, 0.001);
        verify(tripRepository).sumDistanceKmByOwnerIdAndStatus(
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
        when(tripRepository.countByOwnerIdAndMotorcycle(OWNER_ID)).thenReturn(counts);

        Map<Motorcycle, Long> result = statisticsService.countTripsByMotorcycle(OWNER_ID);

        assertSame(counts, result);
        verify(tripRepository).countByOwnerIdAndMotorcycle(OWNER_ID);
    }

    @Test
    void shouldCalculateCompletedDistanceKmByMotorcycle() {
        when(tripRepository.sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        )).thenReturn(100.0);

        double result = statisticsService
                .calculateCompletedDistanceKmByMotorcycle(OWNER_ID, honda);

        assertEquals(100.0, result, 0.001);
        verify(tripRepository).sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                OWNER_ID,
                honda,
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldFindMostUsedMotorcycle() {
        when(tripRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(honda));

        Optional<Motorcycle> result =
                statisticsService.findMostUsedMotorcycleInCompletedTrips(OWNER_ID);

        assertEquals(Optional.of(honda), result);
        verify(tripRepository).findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoCompletedTrips() {
        when(tripRepository.findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());

        Optional<Motorcycle> result =
                statisticsService.findMostUsedMotorcycleInCompletedTrips(OWNER_ID);

        assertTrue(result.isEmpty());
        verify(tripRepository).findMostUsedMotorcycleInCompletedTripsByOwnerId(OWNER_ID);
    }
}
