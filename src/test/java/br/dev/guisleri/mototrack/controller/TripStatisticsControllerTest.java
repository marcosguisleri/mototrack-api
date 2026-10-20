package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
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

    // -------------------------------------------------------------------------
    // GET /statistics - estatísticas consolidadas e estado vazio
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnTripStatistics() throws Exception {
        Motorcycle motorcycle = motorcycle();
        TripStatisticsResponseDTO statistics = new TripStatisticsResponseDTO(
                3L,
                244.5,
                81.5,
                MotorcycleResponseDTO.from(motorcycle),
                TripSummaryResponseDTO.from(trip(
                        10L,
                        5_432.1,
                        LocalDate.of(2025, 1, 15),
                        motorcycle
                )),
                TripSummaryResponseDTO.from(trip(
                        9L,
                        85.0,
                        LocalDate.of(2024, 5, 10),
                        motorcycle
                )),
                TripSummaryResponseDTO.from(trip(
                        11L,
                        120.0,
                        LocalDate.of(2026, 9, 20),
                        motorcycle
                )),
                Map.of(
                        TripStatus.PLANNED, 1L,
                        TripStatus.IN_PROGRESS, 2L,
                        TripStatus.COMPLETED, 3L
                ),
                Map.of(
                        TerrainType.ASPHALT,
                        new TerrainStatistics(2L, 300.0),
                        TerrainType.MIXED,
                        new TerrainStatistics(1L, 150.0),
                        TerrainType.OFF_ROAD,
                        new TerrainStatistics(0L, 0.0)
                ),
                List.of(
                        new MonthlyTripStatistics(2025, 10, 2L, 250.0),
                        new MonthlyTripStatistics(2025, 11, 1L, 300.0),
                        new MonthlyTripStatistics(2025, 12, 0L, 0.0)
                )
        );
        when(tripStatisticsService.getStatistics(CURRENT_USER.getId()))
                .thenReturn(statistics);

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(3))
                .andExpect(jsonPath("$.totalCompletedDistance").value(244.5))
                .andExpect(jsonPath("$.averageCompletedDistanceKm").value(81.5))
                .andExpect(jsonPath("$.mostUsedMotorcycle.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.brand").value("Honda"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.model").value("CB 500X"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.longestTrip.id").value(10))
                .andExpect(jsonPath("$.firstCompletedTrip.id").value(9))
                .andExpect(jsonPath("$.lastCompletedTrip.id").value(11))
                .andExpect(jsonPath("$.tripsByStatus.PLANNED").value(1))
                .andExpect(jsonPath("$.tripsByStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.tripsByStatus.COMPLETED").value(3))
                .andExpect(jsonPath("$.terrainStatistics.ASPHALT.tripCount")
                        .value(2))
                .andExpect(jsonPath("$.terrainStatistics.MIXED.tripCount")
                        .value(1))
                .andExpect(jsonPath("$.terrainStatistics.OFF_ROAD.tripCount")
                        .value(0))
                .andExpect(jsonPath("$.monthlyStatistics.length()").value(3))
                .andExpect(jsonPath("$.monthlyStatistics[0].year").value(2025))
                .andExpect(jsonPath("$.monthlyStatistics[0].month").value(10))
                .andExpect(jsonPath("$.monthlyStatistics[0].tripCount").value(2))
                .andExpect(jsonPath("$.monthlyStatistics[0].totalDistanceKm")
                        .value(250.0));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(tripStatisticsService).getStatistics(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnEmptyTripStatistics() throws Exception {
        TripStatisticsResponseDTO statistics = new TripStatisticsResponseDTO(
                0L,
                0.0,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of(),
                List.of()
        );
        when(tripStatisticsService.getStatistics(CURRENT_USER.getId()))
                .thenReturn(statistics);

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(0))
                .andExpect(jsonPath("$.totalCompletedDistance").value(0.0))
                .andExpect(jsonPath("$.averageCompletedDistanceKm")
                        .value((Object) null))
                .andExpect(jsonPath("$.mostUsedMotorcycle").value((Object) null))
                .andExpect(jsonPath("$.longestTrip").value((Object) null))
                .andExpect(jsonPath("$.firstCompletedTrip").value((Object) null))
                .andExpect(jsonPath("$.lastCompletedTrip").value((Object) null))
                .andExpect(jsonPath("$.tripsByStatus").isEmpty())
                .andExpect(jsonPath("$.terrainStatistics").isEmpty())
                .andExpect(jsonPath("$.monthlyStatistics").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers de criação do domínio
    // -------------------------------------------------------------------------

    private Motorcycle motorcycle() {
        return Motorcycle.restore(
                1L,
                "Honda",
                "CB 500X",
                "Red",
                2023,
                471,
                CURRENT_USER
        );
    }

    private Trip trip(
            Long id,
            double distanceKm,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        return Trip.restore(
                id,
                "Araras",
                "Destination",
                distanceKm,
                TerrainType.MIXED,
                tripDate,
                motorcycle,
                TripStatus.COMPLETED
        );
    }
}
