package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.model.Motorcycle;
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
                User.restore(
                        1L,
                        "Marcos",
                        "marcos@example.com",
                        "password-hash"
                )
        );
        Map<TripStatus, Long> tripsByStatus = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 2L,
                TripStatus.COMPLETED, 3L
        );
        when(tripStatisticsService.countCompletedTrips(CURRENT_USER.getId()))
                .thenReturn(3L);
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(CURRENT_USER.getId()))
                .thenReturn(244.5);
        when(tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(
                CURRENT_USER.getId()
        ))
                .thenReturn(Optional.of(motorcycle));
        when(tripStatisticsService.countTripsByStatus(CURRENT_USER.getId()))
                .thenReturn(tripsByStatus);

        mockMvc.perform(get("/statistics").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(3))
                .andExpect(jsonPath("$.totalCompletedDistance").value(244.5))
                .andExpect(jsonPath("$.mostUsedMotorcycle.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.brand").value("Honda"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.model").value("CB 500X"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.color").value("Red"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.owner.passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.tripsByStatus.PLANNED").value(1))
                .andExpect(jsonPath("$.tripsByStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.tripsByStatus.COMPLETED").value(3));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(tripStatisticsService).countCompletedTrips(CURRENT_USER.getId());
        verify(tripStatisticsService)
                .calculateTotalCompletedDistanceKm(CURRENT_USER.getId());
        verify(tripStatisticsService)
                .findMostUsedMotorcycleInCompletedTrips(CURRENT_USER.getId());
        verify(tripStatisticsService).countTripsByStatus(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNullWhenThereIsNoMostUsedMotorcycle() throws Exception {
        when(tripStatisticsService.countCompletedTrips(CURRENT_USER.getId()))
                .thenReturn(0L);
        when(tripStatisticsService.calculateTotalCompletedDistanceKm(CURRENT_USER.getId()))
                .thenReturn(0.0);
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
