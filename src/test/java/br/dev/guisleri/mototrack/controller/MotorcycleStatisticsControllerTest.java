package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleStatisticsResponseDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleTripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleStatisticsService;
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

@WebMvcTest(MotorcycleStatisticsController.class)
class MotorcycleStatisticsControllerTest {

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
    private MotorcycleStatisticsService motorcycleStatisticsService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldReturnAuthenticatedUsersMotorcycleStatistics() throws Exception {
        Motorcycle motorcycle = Motorcycle.restore(
                10L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                CURRENT_USER
        );
        Map<TerrainType, TerrainStatistics> terrainStatistics = Map.of(
                TerrainType.ASPHALT,
                new TerrainStatistics(2L, 500.0),
                TerrainType.MIXED,
                new TerrainStatistics(0L, 0.0),
                TerrainType.OFF_ROAD,
                new TerrainStatistics(0L, 0.0)
        );
        MotorcycleTripSummaryResponseDTO longestTrip =
                new MotorcycleTripSummaryResponseDTO(
                        20L,
                        "Florianopolis",
                        "Ushuaia",
                        5_000.0,
                        TerrainType.MIXED,
                        LocalDate.of(2025, 1, 15)
                );
        MotorcycleTripSummaryResponseDTO lastCompletedTrip =
                new MotorcycleTripSummaryResponseDTO(
                        21L,
                        "Florianopolis",
                        "Urubici",
                        200.0,
                        TerrainType.ASPHALT,
                        LocalDate.of(2026, 9, 10)
                );
        MotorcycleStatisticsResponseDTO response =
                new MotorcycleStatisticsResponseDTO(
                        MotorcycleResponseDTO.from(motorcycle),
                        2L,
                        500.0,
                        250.0,
                        longestTrip,
                        lastCompletedTrip,
                        terrainStatistics
                );
        when(motorcycleStatisticsService.getStatistics(
                CURRENT_USER.getId(),
                motorcycle.getId()
        )).thenReturn(response);

        mockMvc.perform(get("/motorcycles/10/statistics")
                        .principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motorcycle.id").value(10))
                .andExpect(jsonPath("$.motorcycle.owner.id").value(1))
                .andExpect(jsonPath("$.completedTrips").value(2))
                .andExpect(jsonPath("$.totalCompletedDistanceKm").value(500.0))
                .andExpect(jsonPath("$.averageCompletedDistanceKm").value(250.0))
                .andExpect(jsonPath("$.longestTrip.id").value(20))
                .andExpect(jsonPath("$.longestTrip.distanceKm").value(5_000.0))
                .andExpect(jsonPath("$.longestTrip.motorcycle").doesNotExist())
                .andExpect(jsonPath("$.lastCompletedTrip.id").value(21))
                .andExpect(jsonPath("$.lastCompletedTrip.tripDate")
                        .value("2026-09-10"))
                .andExpect(jsonPath("$.lastCompletedTrip.motorcycle").doesNotExist())
                .andExpect(jsonPath("$.terrainStatistics.ASPHALT.tripCount")
                        .value(2))
                .andExpect(jsonPath("$.terrainStatistics.ASPHALT.totalDistanceKm")
                        .value(500.0));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(motorcycleStatisticsService).getStatistics(
                CURRENT_USER.getId(),
                motorcycle.getId()
        );
    }
}
