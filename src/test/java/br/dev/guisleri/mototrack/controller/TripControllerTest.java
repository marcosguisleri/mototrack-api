package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.TripService;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    private static final String INVALID_REQUEST_BODY_MESSAGE =
            "O corpo da requisição contém valores inválidos ou mal formatados.";
    private static final User CURRENT_USER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com",
            "password-hash"
    );
    private static final Motorcycle PERSISTED_MOTORCYCLE = Motorcycle.restore(
            1L,
            "Honda",
            "NX 500",
            "Black",
            2025,
            471,
            CURRENT_USER
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    CURRENT_USER.getEmail(),
                    null,
                    List.of()
            );

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private MotorcycleService motorcycleService;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldScheduleTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 20);
        Trip trip = plannedTrip(1, tripDate);
        when(motorcycleService.findMotorcycleByIdForOwner(1L, CURRENT_USER.getId()))
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
                        .principal(AUTHENTICATION)
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
                .andExpect(jsonPath("$.motorcycle.color").value("Black"))
                .andExpect(jsonPath("$.motorcycle.owner.id").value(1))
                .andExpect(jsonPath("$.motorcycle.owner.name").value("Marcos"))
                .andExpect(jsonPath("$.motorcycle.owner.email")
                        .value("marcos@example.com"))
                .andExpect(jsonPath("$.motorcycle.owner.password").doesNotExist())
                .andExpect(jsonPath("$.motorcycle.owner.passwordHash").doesNotExist());

        verify(tripService).scheduleTrip(
                eq("Florianopolis"),
                eq("Urubici"),
                eq(175.5),
                eq(TerrainType.MIXED),
                eq(tripDate),
                eq(PERSISTED_MOTORCYCLE)
        );
        verify(motorcycleService).findMotorcycleByIdForOwner(1L, CURRENT_USER.getId());
    }

    @Test
    void shouldRegisterCompletedTrip() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 10);
        Trip trip = completedTrip(2, tripDate);
        when(motorcycleService.findMotorcycleByIdForOwner(1L, CURRENT_USER.getId()))
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
                        .principal(AUTHENTICATION)
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
        verify(motorcycleService).findMotorcycleByIdForOwner(1L, CURRENT_USER.getId());
    }

    @Test
    void shouldFindTripsForOwner() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findTripsByOwnerId(CURRENT_USER.getId()))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));

        verify(tripService).findTripsByOwnerId(CURRENT_USER.getId());
    }

    @Test
    void shouldFindTripsByOwnerAndStatus() throws Exception {
        Trip trip = completedTrip(1, LocalDate.of(2026, 9, 10));
        when(tripService.findTripsByOwnerIdAndStatus(
                CURRENT_USER.getId(),
                TripStatus.COMPLETED
        ))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips")
                        .principal(AUTHENTICATION)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        verify(tripService).findTripsByOwnerIdAndStatus(
                CURRENT_USER.getId(),
                TripStatus.COMPLETED
        );
    }

    @Test
    void shouldFindUpcomingTrips() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findUpcomingTripsByOwnerId(CURRENT_USER.getId()))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/upcoming").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].tripDate").value("2026-09-20"));

        verify(tripService).findUpcomingTripsByOwnerId(CURRENT_USER.getId());
    }

    @Test
    void shouldFindTripsByTerrain() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findTripsByOwnerIdAndTerrain(
                CURRENT_USER.getId(),
                TerrainType.ASPHALT
        ))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/terrain/ASPHALT").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(tripService).findTripsByOwnerIdAndTerrain(
                CURRENT_USER.getId(),
                TerrainType.ASPHALT
        );
    }

    @Test
    void shouldConvertDateAndFindTripsByDate() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 20);
        Trip trip = plannedTrip(1, tripDate);
        when(tripService.findTripsByOwnerIdAndDate(CURRENT_USER.getId(), tripDate))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/trips/date/2026-09-20").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tripDate").value("2026-09-20"));

        verify(tripService).findTripsByOwnerIdAndDate(
                CURRENT_USER.getId(),
                LocalDate.of(2026, 9, 20)
        );
    }

    @Test
    void shouldCalculateDaysUntilTrip() throws Exception {
        when(tripService.calculateDaysUntilTripForOwner(1, CURRENT_USER.getId()))
                .thenReturn(5L);

        mockMvc.perform(get("/trips/1/days-until").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(tripService).calculateDaysUntilTripForOwner(1, CURRENT_USER.getId());
    }

    @Test
    void shouldFindTripById() throws Exception {
        Trip trip = plannedTrip(1, LocalDate.of(2026, 9, 20));
        when(tripService.findTripByIdForOwner(1L, CURRENT_USER.getId()))
                .thenReturn(trip);

        mockMvc.perform(get("/trips/1").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.destination").value("Urubici"));
    }

    @Test
    void shouldReturnNotFoundWhenTripDoesNotExist() throws Exception {
        when(tripService.findTripByIdForOwner(999L, CURRENT_USER.getId())).thenThrow(
                new TripNotFoundException("Viagem com id 999 não encontrada")
        );

        mockMvc.perform(get("/trips/999").principal(AUTHENTICATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Viagem com id 999 não encontrada"));
    }

    @Test
    void shouldReturnForbiddenWhenTripBelongsToAnotherUser() throws Exception {
        when(tripService.findTripByIdForOwner(1L, CURRENT_USER.getId())).thenThrow(
                new TripAccessDeniedException(
                        "Você não possui permissão para acessar esta viagem."
                )
        );

        mockMvc.perform(get("/trips/1").principal(AUTHENTICATION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(
                        "Você não possui permissão para acessar esta viagem."
                ));
    }

    @Test
    void shouldReturnBadRequestWhenTripDateIsInvalid() throws Exception {
        LocalDate tripDate = LocalDate.of(2026, 9, 10);
        when(motorcycleService.findMotorcycleByIdForOwner(1L, CURRENT_USER.getId()))
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
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Não é possível planejar uma viagem para uma data passada."
                ));
    }

    @Test
    void shouldRejectInvalidCreateTripRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post("/trips")
                        .principal(AUTHENTICATION)
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.origin").value("A origem é obrigatória."))
                .andExpect(jsonPath("$.errors.destination")
                        .value("O destino é obrigatório."))
                .andExpect(jsonPath("$.errors.distanceKm")
                        .value("A distância deve ser maior que zero."))
                .andExpect(jsonPath("$.errors.terrain")
                        .value("O tipo de terreno é obrigatório."))
                .andExpect(jsonPath("$.errors.tripDate")
                        .value("A data da viagem é obrigatória."))
                .andExpect(jsonPath("$.errors.motorcycleId")
                        .value("A motocicleta é obrigatória."));

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldRejectNonPositiveMotorcycleIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/trips")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTripJson("2026-09-20", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.motorcycleId")
                        .value("O identificador da motocicleta deve ser maior que zero."));

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldReturnGenericBadRequestForInvalidTerrainEnum() throws Exception {
        assertUnreadableTripPayload(
                createTripJson("2026-09-20")
                        .replace("\"MIXED\"", "\"UNSUPPORTED\"")
        );
    }

    @Test
    void shouldReturnGenericBadRequestForMalformedLocalDate() throws Exception {
        assertUnreadableTripPayload(createTripJson("20/09/2026"));
    }

    @Test
    void shouldReturnGenericBadRequestForMalformedJson() throws Exception {
        assertUnreadableTripPayload("""
                {
                  "origin": "Florianopolis"
                  "destination": "Urubici"
                }
                """);
    }

    @Test
    void shouldReturnGenericBadRequestForIncompatibleType() throws Exception {
        assertUnreadableTripPayload(
                createTripJson("2026-09-20")
                        .replace("\"distanceKm\": 175.5", "\"distanceKm\": \"far\"")
        );
    }

    @Test
    void shouldReturnBadRequestWhenTripStatusIsInvalid() throws Exception {
        doThrow(new InvalidTripStatusException("Status inválido!"))
                .when(tripService)
                .changeTripStatusForOwner(
                        1,
                        CURRENT_USER.getId(),
                        TripStatus.COMPLETED
                );

        mockMvc.perform(patch("/trips/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Status inválido!"));
    }

    @Test
    void shouldRejectNullStatusBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/trips/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.status")
                        .value("O status da viagem é obrigatório."));

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldChangeTripStatus() throws Exception {
        mockMvc.perform(patch("/trips/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tripService).changeTripStatusForOwner(
                1,
                CURRENT_USER.getId(),
                TripStatus.IN_PROGRESS
        );
    }

    @Test
    void shouldDeleteTripById() throws Exception {
        mockMvc.perform(delete("/trips/1").principal(AUTHENTICATION))
                .andExpect(status().isNoContent());

        verify(tripService).deleteTripByIdForOwner(1L, CURRENT_USER.getId());
    }

    private void assertUnreadableTripPayload(String payload) throws Exception {
        mockMvc.perform(post("/trips")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(INVALID_REQUEST_BODY_MESSAGE))
                .andExpect(content().string(not(containsString("Jackson"))))
                .andExpect(content().string(not(containsString("InvalidFormatException"))))
                .andExpect(content().string(not(containsString("TerrainType"))));

        verifyNoInteractions(tripService, motorcycleService);
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
