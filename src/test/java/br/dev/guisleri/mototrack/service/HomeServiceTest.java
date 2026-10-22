package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.HomeResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private TripService tripService;

    @Mock
    private TripStatisticsService tripStatisticsService;

    @Mock
    private MotorcycleService motorcycleService;

    private HomeService homeService;
    private User owner;
    private Motorcycle honda;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(
                tripService,
                tripStatisticsService,
                motorcycleService
        );
        owner = User.restore(
                OWNER_ID,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        honda = motorcycle(1L, "Honda", "NX 500");
    }

    // -------------------------------------------------------------------------
    // Composição dos dados e estados opcionais da tela inicial
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnHomeData() {
        Trip nextTrip = trip(
                1L,
                LocalDate.of(2026, 10, 5),
                TripStatus.PLANNED
        );
        Trip lastCompletedTrip = trip(
                2L,
                LocalDate.of(2026, 9, 10),
                TripStatus.COMPLETED
        );
        Trip inProgressTrip = trip(
                3L,
                LocalDate.of(2026, 9, 14),
                TripStatus.IN_PROGRESS
        );
        when(tripService.findNextUpcomingTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(nextTrip));
        when(tripService.calculateDaysUntilTrip(nextTrip))
                .thenReturn(8L);
        when(tripStatisticsService.findLastCompletedTrip(OWNER_ID))
                .thenReturn(Optional.of(lastCompletedTrip));
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(OWNER_ID))
                .thenReturn(1_250.5);
        when(tripStatisticsService.countCompletedTrips(OWNER_ID))
                .thenReturn(4L);
        when(tripService.findInProgressTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(inProgressTrip));
        when(tripStatisticsService.calculateCompletedDistanceKmForCurrentYear(OWNER_ID))
                .thenReturn(980.0);
        when(motorcycleService.countMotorcyclesByOwnerId(OWNER_ID))
                .thenReturn(2L);

        HomeResponseDTO home = homeService.getHome(OWNER_ID);

        assertNotNull(home.nextTrip());
        assertEquals(nextTrip.getId(), home.nextTrip().id());
        assertEquals(8L, home.nextTrip().daysUntil());
        assertNotNull(home.inProgressTrip());
        assertEquals(inProgressTrip.getId(), home.inProgressTrip().id());
        assertNotNull(home.lastCompletedTrip());
        assertEquals(lastCompletedTrip.getId(), home.lastCompletedTrip().id());
        assertEquals(1_250.5, home.totalCompletedDistanceKm(), 0.001);
        assertEquals(980.0, home.distanceThisYearKm(), 0.001);
        assertEquals(4L, home.completedTrips());
        assertEquals(2L, home.motorcycleCount());
        verify(tripService).findNextUpcomingTripByOwnerId(OWNER_ID);
        verify(tripService).calculateDaysUntilTrip(nextTrip);
        verify(tripStatisticsService).findLastCompletedTrip(OWNER_ID);
        verify(tripStatisticsService).calculateTotalCompletedDistanceKm(OWNER_ID);
        verify(tripStatisticsService).countCompletedTrips(OWNER_ID);
        verify(tripService).findInProgressTripByOwnerId(OWNER_ID);
        verify(tripStatisticsService)
                .calculateCompletedDistanceKmForCurrentYear(OWNER_ID);
        verify(motorcycleService).countMotorcyclesByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnNullNextTripWhenThereAreNoUpcomingTrips() {
        when(tripService.findNextUpcomingTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.findLastCompletedTrip(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(OWNER_ID))
                .thenReturn(0.0);
        when(tripStatisticsService.countCompletedTrips(OWNER_ID))
                .thenReturn(0L);
        when(motorcycleService.countMotorcyclesByOwnerId(OWNER_ID))
                .thenReturn(0L);

        HomeResponseDTO home = homeService.getHome(OWNER_ID);

        assertNull(home.nextTrip());
        verify(tripService).findNextUpcomingTripByOwnerId(OWNER_ID);
        verify(tripService, never())
                .calculateDaysUntilTrip(any(Trip.class));
    }

    @Test
    void shouldReturnNullLastCompletedTripWhenThereAreNoCompletedTrips() {
        when(tripService.findNextUpcomingTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.findLastCompletedTrip(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(OWNER_ID))
                .thenReturn(0.0);
        when(tripStatisticsService.countCompletedTrips(OWNER_ID))
                .thenReturn(0L);
        when(motorcycleService.countMotorcyclesByOwnerId(OWNER_ID))
                .thenReturn(0L);

        HomeResponseDTO home = homeService.getHome(OWNER_ID);

        assertNotNull(home);
        assertNull(home.lastCompletedTrip());
        verify(tripStatisticsService).findLastCompletedTrip(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Seleção da próxima viagem
    // -------------------------------------------------------------------------

    @Test
    void shouldUseNextUpcomingTrip() {
        Trip nearestTrip = trip(
                1L,
                LocalDate.of(2026, 10, 5),
                TripStatus.PLANNED
        );
        when(tripService.findNextUpcomingTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.of(nearestTrip));
        when(tripService.calculateDaysUntilTrip(nearestTrip))
                .thenReturn(8L);
        when(tripStatisticsService.findLastCompletedTrip(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(OWNER_ID))
                .thenReturn(0.0);
        when(tripStatisticsService.countCompletedTrips(OWNER_ID))
                .thenReturn(0L);
        when(motorcycleService.countMotorcyclesByOwnerId(OWNER_ID))
                .thenReturn(1L);

        HomeResponseDTO home = homeService.getHome(OWNER_ID);

        assertNotNull(home.nextTrip());
        assertEquals(nearestTrip.getId(), home.nextTrip().id());
        verify(tripService).calculateDaysUntilTrip(nearestTrip);
    }

    // -------------------------------------------------------------------------
    // Contagem de motocicletas
    // -------------------------------------------------------------------------

    @Test
    void shouldCountMotorcycles() {
        when(tripService.findNextUpcomingTripByOwnerId(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.findLastCompletedTrip(OWNER_ID))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(OWNER_ID))
                .thenReturn(0.0);
        when(tripStatisticsService.countCompletedTrips(OWNER_ID))
                .thenReturn(0L);
        when(motorcycleService.countMotorcyclesByOwnerId(OWNER_ID))
                .thenReturn(3L);

        HomeResponseDTO home = homeService.getHome(OWNER_ID);

        assertEquals(3L, home.motorcycleCount());
        verify(motorcycleService).countMotorcyclesByOwnerId(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Helpers de domínio
    // -------------------------------------------------------------------------

    private Trip trip(
            Long id,
            LocalDate tripDate,
            TripStatus status
    ) {
        return Trip.restore(
                id,
                "Origem " + id,
                "Destino " + id,
                100.0,
                TerrainType.ASPHALT,
                tripDate,
                honda,
                status,
                null,
                null
        );
    }

    private Motorcycle motorcycle(Long id, String brand, String model) {
        return Motorcycle.restore(
                id,
                brand,
                model,
                "Black",
                2025,
                500,
                MotorcycleType.NAKED,
                owner
        );
    }
}
