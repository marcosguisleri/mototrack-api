package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
class TripControllerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );
    private static final Motorcycle MOTORCYCLE = new Motorcycle(
            1,
            "Honda",
            "NX 500",
            2025,
            471
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @Test
    void shouldScheduleTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 20);
        Trip trip = plannedTrip(1, tripDate);
        when(tripService.scheduleTrip(
                1,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE
        )).thenReturn(trip);

        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson(1, "2026-09-20")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.origin").value("Florianopolis"))
                .andExpect(jsonPath("$.destination").value("Urubici"))
                .andExpect(jsonPath("$.distanceKm").value(175.5))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.terrain").value("MIXED"))
                .andExpect(jsonPath("$.tripDate").value("2026-09-20"))
                .andExpect(jsonPath("$.motorcycle.id").value(1));

        verify(tripService).scheduleTrip(
                1,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE
        );
    }

    @Test
    void shouldRegisterCompletedTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 10);
        Trip trip = completedTrip(2, tripDate);
        when(tripService.registerCompletedTrip(
                2,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE
        )).thenReturn(trip);

        mockMvc.perform(post("/trips/completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson(2, "2026-09-10")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(tripService).registerCompletedTrip(
                2,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE
        );
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
        when(tripService.scheduleTrip(
                3,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE
        )).thenThrow(new InvalidTripDateException(
                "Não é possível planejar uma viagem para uma data passada."
        ));

        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson(3, "2026-09-10")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Não é possível planejar uma viagem para uma data passada."
                ));
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

    private Trip plannedTrip(long id, LocalDate tripDate) {
        return Trip.schedule(
                id,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE,
                FIXED_CLOCK
        );
    }

    private Trip completedTrip(long id, LocalDate tripDate) {
        return Trip.registerCompleted(
                id,
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                tripDate,
                MOTORCYCLE,
                FIXED_CLOCK
        );
    }

    private String createTripJson(long id, String tripDate) {
        return """
                {
                  "id": %d,
                  "origin": "Florianopolis",
                  "destination": "Urubici",
                  "distanceKm": 175.5,
                  "terrain": "MIXED",
                  "tripDate": "%s",
                  "motorcycle": {
                    "id": 1,
                    "brand": "Honda",
                    "model": "NX 500",
                    "year": 2025,
                    "engineCapacity": 471
                  }
                }
                """.formatted(id, tripDate);
    }
}
