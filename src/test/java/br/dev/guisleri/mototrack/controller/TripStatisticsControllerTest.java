package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.TripStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripStatisticsController.class)
class TripStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripStatisticsService tripStatisticsService;

    @Test
    void shouldReturnTripStatistics() throws Exception {
        Motorcycle motorcycle = new Motorcycle(
                1,
                "Honda",
                "CB 500X",
                2023,
                471
        );
        Map<TripStatus, Long> tripsByStatus = Map.of(
                TripStatus.PLANNED, 1L,
                TripStatus.IN_PROGRESS, 2L,
                TripStatus.COMPLETED, 3L
        );
        when(tripStatisticsService.countCompletedTrips()).thenReturn(3L);
        when(tripStatisticsService.calculateTotalCompletedDistance()).thenReturn(244.5);
        when(tripStatisticsService.getMostUsedMotorcycleInCompletedTrips())
                .thenReturn(Optional.of(motorcycle));
        when(tripStatisticsService.countTripsByStatus()).thenReturn(tripsByStatus);

        mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(3))
                .andExpect(jsonPath("$.totalCompletedDistance").value(244.5))
                .andExpect(jsonPath("$.mostUsedMotorcycle.id").value(1))
                .andExpect(jsonPath("$.mostUsedMotorcycle.brand").value("Honda"))
                .andExpect(jsonPath("$.mostUsedMotorcycle.model").value("CB 500X"))
                .andExpect(jsonPath("$.tripsByStatus.PLANNED").value(1))
                .andExpect(jsonPath("$.tripsByStatus.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.tripsByStatus.COMPLETED").value(3));

        verify(tripStatisticsService).countCompletedTrips();
        verify(tripStatisticsService).calculateTotalCompletedDistance();
        verify(tripStatisticsService).getMostUsedMotorcycleInCompletedTrips();
        verify(tripStatisticsService).countTripsByStatus();
    }

    @Test
    void shouldReturnNullWhenThereIsNoMostUsedMotorcycle() throws Exception {
        when(tripStatisticsService.countCompletedTrips()).thenReturn(0L);
        when(tripStatisticsService.calculateTotalCompletedDistance()).thenReturn(0.0);
        when(tripStatisticsService.getMostUsedMotorcycleInCompletedTrips())
                .thenReturn(Optional.empty());
        when(tripStatisticsService.countTripsByStatus()).thenReturn(Map.of());

        mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedTrips").value(0))
                .andExpect(jsonPath("$.totalCompletedDistance").value(0.0))
                .andExpect(jsonPath("$.mostUsedMotorcycle").value((Object) null))
                .andExpect(jsonPath("$.tripsByStatus").isEmpty());

        verify(tripStatisticsService).getMostUsedMotorcycleInCompletedTrips();
    }

    @Test
    void shouldRoundTotalDistanceToOneDecimalPlace() throws Exception {
        when(tripStatisticsService.calculateTotalCompletedDistance())
                .thenReturn(111.10000000000001);
        when(tripStatisticsService.getMostUsedMotorcycleInCompletedTrips())
                .thenReturn(Optional.empty());
        when(tripStatisticsService.countTripsByStatus()).thenReturn(Map.of());

        mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedDistance").value(111.1));
    }
}
