package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.FuelEntryNotFoundException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.FuelEntryService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FuelEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class FuelEntryControllerTest {

    private static final User CURRENT_USER = User.restore(
            1L, "Marcos", "marcos@example.com", "password-hash"
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    CURRENT_USER.getEmail(), null, List.of()
            );
    private static final String VALID_BODY = """
            {
              "liters": 10.2,
              "totalCost": 64.90
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FuelEntryService fuelEntryService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldRegisterFuelEntry() throws Exception {
        when(fuelEntryService.registerFuelEntryForOwner(
                5L, CURRENT_USER.getId(), new BigDecimal("10.2"), new BigDecimal("64.90")
        )).thenReturn(fuelEntry(9L));

        mockMvc.perform(post("/trips/5/fuel-entries")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.liters").value(10.2))
                .andExpect(jsonPath("$.totalCost").value(64.90))
                .andExpect(jsonPath("$.pricePerLiter").value(6.36))
                .andExpect(jsonPath("$.createdAt").value("2026-09-14T12:00:00Z"));
    }

    @Test
    void shouldRejectInvalidFuelEntryPayload() throws Exception {
        mockMvc.perform(post("/trips/5/fuel-entries")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "liters": 0,
                                  "totalCost": 12345.678
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(fuelEntryService);
    }

    @Test
    void shouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/trips/5/fuel-entries")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fuelEntryService);
    }

    @Test
    void shouldReturnBadRequestWhenTripIsPlanned() throws Exception {
        when(fuelEntryService.registerFuelEntryForOwner(
                5L, CURRENT_USER.getId(), new BigDecimal("10.2"), new BigDecimal("64.90")
        )).thenThrow(new InvalidTripStatusException("Viagem planejada."));

        mockMvc.perform(post("/trips/5/fuel-entries")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Viagem planejada."));
    }

    @Test
    void shouldReturnForbiddenForForeignTrip() throws Exception {
        when(fuelEntryService.findFuelEntriesByTripIdForOwner(5L, CURRENT_USER.getId()))
                .thenThrow(new TripAccessDeniedException("negado"));

        mockMvc.perform(get("/trips/5/fuel-entries").principal(AUTHENTICATION))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFindFuelEntries() throws Exception {
        when(fuelEntryService.findFuelEntriesByTripIdForOwner(5L, CURRENT_USER.getId()))
                .thenReturn(List.of(fuelEntry(1L), fuelEntry(2L)));

        mockMvc.perform(get("/trips/5/fuel-entries").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void shouldUpdateFuelEntry() throws Exception {
        when(fuelEntryService.updateFuelEntryForOwner(
                5L, 9L, CURRENT_USER.getId(), new BigDecimal("10.2"), new BigDecimal("64.90")
        )).thenReturn(fuelEntry(9L));

        mockMvc.perform(put("/trips/5/fuel-entries/9")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingFuelEntry() throws Exception {
        when(fuelEntryService.updateFuelEntryForOwner(
                5L, 9L, CURRENT_USER.getId(), new BigDecimal("10.2"), new BigDecimal("64.90")
        )).thenThrow(new FuelEntryNotFoundException("Abastecimento não encontrado"));

        mockMvc.perform(put("/trips/5/fuel-entries/9")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteFuelEntry() throws Exception {
        mockMvc.perform(delete("/trips/5/fuel-entries/9").principal(AUTHENTICATION))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(fuelEntryService).deleteFuelEntryForOwner(5L, 9L, CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingFuelEntry() throws Exception {
        doThrow(new FuelEntryNotFoundException("Abastecimento não encontrado"))
                .when(fuelEntryService)
                .deleteFuelEntryForOwner(5L, 9L, CURRENT_USER.getId());

        mockMvc.perform(delete("/trips/5/fuel-entries/9").principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }

    private FuelEntry fuelEntry(long id) {
        return FuelEntry.restore(
                id,
                5L,
                new BigDecimal("10.2"),
                new BigDecimal("64.90"),
                Instant.parse("2026-09-14T12:00:00Z")
        );
    }
}
