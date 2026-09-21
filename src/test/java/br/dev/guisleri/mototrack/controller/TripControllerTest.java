package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
class TripControllerTest {

    private static final Motorcycle PERSISTED_MOTORCYCLE = Motorcycle.restore(
            1L,
            "Honda",
            "NX 500",
            "Black",
            2025,
            471
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private MotorcycleService motorcycleService;

    @Test
    void shouldScheduleTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 20);
        Trip trip = plannedTrip(1, tripDate);
        when(motorcycleService.findMotorcycleById(1L))
                .thenReturn(PERSISTED_MOTORCYCLE);
        when(tripService.scheduleTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        )).thenReturn(trip);

        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-20")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.origin").value("Florianopolis"))
                .andExpect(jsonPath("$.destination").value("Urubici"))
                .andExpect(jsonPath("$.distanceKm").value(175.5))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.terrain").value("MIXED"))
                .andExpect(jsonPath("$.tripDate").value("2026-09-20"))
                .andExpect(jsonPath("$.motorcycle.id").value(1))
                .andExpect(jsonPath("$.motorcycle.color").value("Black"));

        verify(tripService).scheduleTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        );
        verify(motorcycleService).findMotorcycleById(1L);
    }

    @Test
    void shouldRegisterCompletedTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 10);
        Trip trip = completedTrip(2, tripDate);
        when(motorcycleService.findMotorcycleById(1L))
                .thenReturn(PERSISTED_MOTORCYCLE);
        when(tripService.registerCompletedTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        )).thenReturn(trip);

        mockMvc.perform(post("/trips/completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(tripService).registerCompletedTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        );
        verify(motorcycleService).findMotorcycleById(1L);
    }

    @Test
    void shouldFindAllTrips() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findAllTrips()).thenReturn(List.of(trip));

        mockMvc.perform(get("/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));

        verify(tripService).findAllTrips();
    }

    @Test
    void shouldFindTripsByStatus() throws Exception {
        Trip trip = completedTrip(1, LocalDate.of(2026, 9, 10));
        when(tripService.findTripsByStatus(TripStatus.COMPLETED))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        verify(tripService).findTripsByStatus(TripStatus.COMPLETED);
    }

    @Test
    void shouldFindUpcomingTrips() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findUpcomingTrips()).thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tripDate").value("2026-09-20"));

        verify(tripService).findUpcomingTrips();
    }

    @Test
    void shouldFindTripsByTerrain() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findTripsByTerrain(TerrainType.ASPHALT))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/terrain/ASPHALT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(tripService).findTripsByTerrain(TerrainType.ASPHALT);
    }

    @Test
    void shouldConvertDateAndFindTripsByDate() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 20);
        Trip trip = plannedTrip(1, tripDate);
        when(tripService.findTripsByDate(tripDate)).thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/date/2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tripDate").value("2026-09-20"));

        verify(tripService).findTripsByDate(LocalDate.of(2026, 9, 20));
    }

    @Test
    void shouldCalculateDaysUntilTrip() throws Exception {
        when(tripService.calculateDaysUntilTrip(1)).thenReturn(5L);

        mockMvc.perform(get("/trips/1/days-until"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(tripService).calculateDaysUntilTrip(1);
    }

    @Test
    void shouldFindTripById() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findTripById(1)).thenReturn(trip);

        mockMvc.perform(get("/trips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.destination").value("Urubici"));
    }

    @Test
    void shouldReturnNotFoundWhenTripDoesNotExist() throws Exception {
        when(tripService.findTripById(999)).thenThrow(
                new TripNotFoundException("Viagem com id 999 não encontrada")
        );

        mockMvc.perform(get("/trips/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Viagem com id 999 não encontrada"));
    }

    @Test
    void shouldReturnBadRequestWhenTripDateIsInvalid() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 10);
        when(motorcycleService.findMotorcycleById(1L))
                .thenReturn(PERSISTED_MOTORCYCLE);
        when(tripService.scheduleTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        )).thenThrow(new InvalidTripDateException(
                "Não é possível planejar uma viagem para uma data passada."
        ));

        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-10")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Não é possível planejar uma viagem para uma data passada."
                ));
    }

    @Test
    void shouldRejectInvalidCreateTripRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": " ",
                                  "destination": "",
                                  "distanceKm": 0,
                                  "terrain": null,
                                  "tripDate": null,
                                  "motorcycleId": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldRejectNonPositiveMotorcycleIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-20", 0)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldReturnBadRequestWhenTripStatusIsInvalid() throws Exception {
        doThrow(new InvalidTripStatusException("Status inválido!"))
                .when(tripService)
                .changeTripStatus(1, TripStatus.COMPLETED);

        mockMvc.perform(patch("/trips/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Status inválido!"));
    }

    @Test
    void shouldRejectNullStatusBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/trips/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldChangeTripStatus() throws Exception {
        mockMvc.perform(patch("/trips/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tripService).changeTripStatus(1, TripStatus.IN_PROGRESS);
    }

    @Test
    void shouldDeleteTripById() throws Exception {
        mockMvc.perform(delete("/trips/1"))
                .andExpect(status().isNoContent());

        verify(tripService).deleteTripById(1L);
    }

    private Trip plannedTrip(long tripId, LocalDate tripDate) {
        return Trip.restore(
                tripId,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                PERSISTED_MOTORCYCLE,
                TripStatus.PLANNED
        );
    }

    private Trip completedTrip(long tripId, LocalDate tripDate) {
        return Trip.restore(
                tripId,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                PERSISTED_MOTORCYCLE,
                TripStatus.COMPLETED
        );
    }

    private String createTripJson(String tripDate) {
        return createTripJson(tripDate, 1);
    }

    private String createTripJson(String tripDate, long motorcycleId) {
        return """
                {
                  "origin": "Florianopolis",
                  "destination": "Urubici",
                  "distanceKm": 175.5,
                  "terrain": "MIXED",
                  "tripDate": "%s",
                  "motorcycleId": %d
                }
                """.formatted(tripDate, motorcycleId);
    }
}
