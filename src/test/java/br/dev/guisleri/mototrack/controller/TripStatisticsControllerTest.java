package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripStatisticsService;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripStatisticsController.class)
class TripStatisticsControllerTest {

    private static final User CURRENT_USER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com",
            "password-hash"
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    CURRENT_USER.getEmail(),
                    null,
                    List.of()
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripStatisticsService tripStatisticsService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldReturnTripStatistics() throws Exception {
        Motorcycle motorcycle = Motorcycle.restore(
                1L,
                "Honda",
                "CB 500X",
                "Red",
                2023,
                471,
                CURRENT_USER
        );
        Trip longestTrip = Trip.restore(
                10L,
                "Florianopolis",
                "Ushuaia",
                5432.1,
                TerrainType.MIXED,
                LocalDate.of(2025, 1, 15),
                motorcycle,
                TripStatus.COMPLETED
        );
        Trip firstCompletedTrip = Trip.restore(
                9L,
                "Araras",
                "Rio Claro",
                85.0,
                TerrainType.ASPHALT,
                LocalDate.of(2024, 5, 10),
                motorcycle,
                TripStatus.COMPLETED
        );
        Trip lastCompletedTrip = Trip.restore(
                11L,
                "Araras",
                "Campinas",
                120.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 20),
                motorcycle,
                TripStatus.COMPLETED
        );
        Map<TripStatus, Long> tripsByStatus = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 2L,
                TripStatus.COMPLETED, 3L
        );
        Map<TerrainType, TerrainStatistics> terrainStatistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(2L, 300.0),
                TerrainType.MIXED,
                new TerrainStatistics(1L, 150.0),
                TerrainType.OFF_ROAD,
                new TerrainStatistics(0L, 0.0)
        );
        when(tripStatisticsService.countCompletedTrips(CURRENT_USER.getId()))
                .thenReturn(3L);
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(CURRENT_USER.getId()))
                .thenReturn(244.5);
        when(tripStatisticsService.calculateAverageCompletedDistanceKm(
                CURRENT_USER.getId()
        ))
                .thenReturn(81.5);
        when(tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(
                CURRENT_USER.getId()
        ))
                .thenReturn(Optional.of(motorcycle));
        when(tripStatisticsService.findLongestCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.of(longestTrip));
        when(tripStatisticsService.findFirstCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.of(firstCompletedTrip));
        when(tripStatisticsService.findLastCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.of(lastCompletedTrip));
        when(tripStatisticsService.countTripsByStatus(CURRENT_USER.getId()))
                .thenReturn(tripsByStatus);
        when(tripStatisticsService.findTerrainStatistics(CURRENT_USER.getId()))
                .thenReturn(terrainStatistics);

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(3))
                .andExpect(jsonPath("$.totalCompletedDistance").value(244.5))
                .andExpect(jsonPath("$.averageCompletedDistanceKm").value(81.5))
                .andExpect(jsonPath("$.mostUsedMotorcycle.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.brand").value("Honda"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.model").value("CB 500X"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.color").value("Red"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.longestTrip.id").value(10))
                .andExpect(jsonPath("$.longestTrip.distanceKm").value(5432.1))
                .andExpect(jsonPath("$.longestTrip.motorcycle.id").value(1))
                .andExpect(jsonPath("$.firstCompletedTrip.id").value(9))
                .andExpect(jsonPath("$.firstCompletedTrip.distanceKm").value(85.0))
                .andExpect(jsonPath("$.firstCompletedTrip.motorcycle.id").value(1))
                .andExpect(jsonPath("$.lastCompletedTrip.id").value(11))
                .andExpect(jsonPath("$.lastCompletedTrip.distanceKm").value(120.0))
                .andExpect(jsonPath("$.lastCompletedTrip.motorcycle.id").value(1))
                .andExpect(jsonPath("$.tripsByStatus.PLANNED").value(1))
                .andExpect(jsonPath("$.tripsByStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.tripsByStatus.COMPLETED").value(3))
                .andExpect(jsonPath("$.terrainStatistics.ASPHALT.tripCount")
                        .value(2))
                .andExpect(jsonPath("$.terrainStatistics.ASPHALT.totalDistanceKm")
                        .value(300.0))
                .andExpect(jsonPath("$.terrainStatistics.MIXED.tripCount")
                        .value(1))
                .andExpect(jsonPath("$.terrainStatistics.OFF_ROAD.tripCount")
                        .value(0));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(tripStatisticsService).countCompletedTrips(CURRENT_USER.getId());
        verify(tripStatisticsService)
                .calculateTotalCompletedDistanceKm(CURRENT_USER.getId());
        verify(tripStatisticsService)
                .calculateAverageCompletedDistanceKm(CURRENT_USER.getId());
        verify(tripStatisticsService)
                .findMostUsedMotorcycleInCompletedTrips(CURRENT_USER.getId());
        verify(tripStatisticsService).findLongestCompletedTrip(CURRENT_USER.getId());
        verify(tripStatisticsService).findFirstCompletedTrip(CURRENT_USER.getId());
        verify(tripStatisticsService).findLastCompletedTrip(CURRENT_USER.getId());
        verify(tripStatisticsService).countTripsByStatus(CURRENT_USER.getId());
        verify(tripStatisticsService).findTerrainStatistics(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNullWhenThereIsNoLongestCompletedTrip() throws Exception {
        when(tripStatisticsService.findLongestCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longestTrip").value((Object) null));

        verify(tripStatisticsService).findLongestCompletedTrip(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNullWhenThereIsNoFirstCompletedTrip() throws Exception {
        when(tripStatisticsService.findFirstCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstCompletedTrip").value((Object) null));

        verify(tripStatisticsService).findFirstCompletedTrip(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNullWhenThereIsNoLastCompletedTrip() throws Exception {
        when(tripStatisticsService.findLastCompletedTrip(CURRENT_USER.getId()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCompletedTrip").value((Object) null));

        verify(tripStatisticsService).findLastCompletedTrip(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNullWhenThereIsNoMostUsedMotorcycle() throws Exception {
        when(tripStatisticsService.countCompletedTrips(CURRENT_USER.getId()))
                .thenReturn(0L);
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(CURRENT_USER.getId()))
                .thenReturn(0.0);
        when(tripStatisticsService.calculateAverageCompletedDistanceKm(
                CURRENT_USER.getId()
        ))
                .thenReturn(null);
        when(tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(
                CURRENT_USER.getId()
        ))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.countTripsByStatus(CURRENT_USER.getId()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(0))
                .andExpect(jsonPath("$.totalCompletedDistance").value(0.0))
                .andExpect(jsonPath("$.averageCompletedDistanceKm")
                        .value((Object) null))
                .andExpect(jsonPath("$.mostUsedMotorcycle").value((Object) null))
                .andExpect(jsonPath("$.tripsByStatus").isEmpty());

        verify(tripStatisticsService)
                .findMostUsedMotorcycleInCompletedTrips(CURRENT_USER.getId());
    }

    @Test
    void shouldRoundTotalDistanceKmToOneDecimalPlace() throws Exception {
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(CURRENT_USER.getId()))
                .thenReturn(111.10000000000001);
        when(tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(
                CURRENT_USER.getId()
        ))
                .thenReturn(Optional.empty());
        when(tripStatisticsService.countTripsByStatus(CURRENT_USER.getId()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedDistance").value(111.1));
    }
}
