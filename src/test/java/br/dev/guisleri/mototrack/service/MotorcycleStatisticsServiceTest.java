package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.dto.MotorcycleStatisticsResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotorcycleStatisticsServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MOTORCYCLE_ID = 10L;

    @Mock
    private MotorcycleService motorcycleService;

    @Mock
    private TripStatisticsService tripStatisticsService;

    private MotorcycleStatisticsService motorcycleStatisticsService;
    private Motorcycle motorcycle;

    @BeforeEach
    void setUp() {
        motorcycleStatisticsService = new MotorcycleStatisticsService(
                motorcycleService,
                tripStatisticsService
        );
        User owner = User.restore(
                OWNER_ID,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        motorcycle = Motorcycle.restore(
                MOTORCYCLE_ID,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                owner
        );
    }

    @Test
    void shouldBuildStatisticsForOwnersMotorcycle() {
        Map<TerrainType, TerrainStatistics> terrainStatistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(2L, 500.0)
        );
        when(motorcycleService.findMotorcycleByIdForOwner(
                MOTORCYCLE_ID,
                OWNER_ID
        )).thenReturn(motorcycle);
        when(tripStatisticsService.countCompletedTripsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(2L);
        when(tripStatisticsService.calculateCompletedDistanceKmByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(500.0);
        when(tripStatisticsService.findLongestCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.empty());
        when(tripStatisticsService.findLastCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.empty());
        when(tripStatisticsService.findTerrainStatisticsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(terrainStatistics);

        MotorcycleStatisticsResponseDTO result =
                motorcycleStatisticsService.getStatistics(
                        OWNER_ID,
                        MOTORCYCLE_ID
                );

        assertEquals(MOTORCYCLE_ID, result.motorcycle().id());
        assertEquals(2L, result.completedTrips());
        assertEquals(500.0, result.totalCompletedDistanceKm(), 0.001);
        assertEquals(250.0, result.averageCompletedDistanceKm(), 0.001);
        assertNull(result.longestTrip());
        assertNull(result.lastCompletedTrip());
        assertEquals(terrainStatistics, result.terrainStatistics());
        verify(motorcycleService).findMotorcycleByIdForOwner(
                MOTORCYCLE_ID,
                OWNER_ID
        );
    }

    @Test
    void shouldBuildEmptyStatisticsForMotorcycleWithoutCompletedTrips() {
        Map<TerrainType, TerrainStatistics> terrainStatistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(0L, 0.0),
                TerrainType.MIXED,
                new TerrainStatistics(0L, 0.0),
                TerrainType.OFF_ROAD,
                new TerrainStatistics(0L, 0.0)
        );
        when(motorcycleService.findMotorcycleByIdForOwner(
                MOTORCYCLE_ID,
                OWNER_ID
        )).thenReturn(motorcycle);
        when(tripStatisticsService.countCompletedTripsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(0L);
        when(tripStatisticsService.calculateCompletedDistanceKmByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(0.0);
        when(tripStatisticsService.findLongestCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.empty());
        when(tripStatisticsService.findLastCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.empty());
        when(tripStatisticsService.findTerrainStatisticsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(terrainStatistics);

        MotorcycleStatisticsResponseDTO result =
                motorcycleStatisticsService.getStatistics(
                        OWNER_ID,
                        MOTORCYCLE_ID
                );

        assertEquals(0L, result.completedTrips());
        assertEquals(0.0, result.totalCompletedDistanceKm(), 0.001);
        assertNull(result.averageCompletedDistanceKm());
        assertNull(result.longestTrip());
        assertNull(result.lastCompletedTrip());
        assertEquals(terrainStatistics, result.terrainStatistics());
    }

    @Test
    void shouldIncludeLongestAndLastCompletedTrips() {
        Trip longestTrip = Trip.restore(
                20L,
                "Florianopolis",
                "Ushuaia",
                5_000.0,
                TerrainType.MIXED,
                LocalDate.of(2025, 1, 15),
                motorcycle,
                TripStatus.COMPLETED
        );
        Trip lastCompletedTrip = Trip.restore(
                21L,
                "Florianopolis",
                "Urubici",
                200.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 10),
                motorcycle,
                TripStatus.COMPLETED
        );
        when(motorcycleService.findMotorcycleByIdForOwner(
                MOTORCYCLE_ID,
                OWNER_ID
        )).thenReturn(motorcycle);
        when(tripStatisticsService.countCompletedTripsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(2L);
        when(tripStatisticsService.calculateCompletedDistanceKmByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(5_200.0);
        when(tripStatisticsService.findLongestCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.of(longestTrip));
        when(tripStatisticsService.findLastCompletedTripByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Optional.of(lastCompletedTrip));
        when(tripStatisticsService.findTerrainStatisticsByMotorcycle(
                OWNER_ID,
                motorcycle
        )).thenReturn(Map.of());

        MotorcycleStatisticsResponseDTO result =
                motorcycleStatisticsService.getStatistics(
                        OWNER_ID,
                        MOTORCYCLE_ID
                );

        assertEquals(longestTrip.getId(), result.longestTrip().id());
        assertEquals(longestTrip.getDistanceKm(), result.longestTrip().distanceKm());
        assertEquals(lastCompletedTrip.getId(), result.lastCompletedTrip().id());
        assertEquals(
                lastCompletedTrip.getTripDate(),
                result.lastCompletedTrip().tripDate()
        );
    }
}
