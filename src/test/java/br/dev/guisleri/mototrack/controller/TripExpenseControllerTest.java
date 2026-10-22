package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.TripExpenseNotFoundException;
import br.dev.guisleri.mototrack.exception.InvalidTripExpenseException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripExpenseService;
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

@WebMvcTest(TripExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripExpenseControllerTest {

    private static final User CURRENT_USER = User.restore(
            1L, "Marcos", "marcos@example.com", "password-hash"
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    CURRENT_USER.getEmail(), null, List.of()
            );
    private static final String VALID_BODY = """
            {
              "category": "TOLL",
              "amount": 13.90,
              "description": "Praça 2"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripExpenseService tripExpenseService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldRegisterExpense() throws Exception {
        when(tripExpenseService.registerExpenseForOwner(
                5L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("13.90"), "Praça 2"
        )).thenReturn(expense(9L));

        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.category").value("TOLL"))
                .andExpect(jsonPath("$.amount").value(13.90))
                .andExpect(jsonPath("$.description").value("Praça 2"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-14T12:00:00Z"));
    }

    @Test
    void shouldRejectInvalidExpensePayload() throws Exception {
        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "FOOD",
                                  "amount": -1,
                                  "description": "%s"
                                }
                                """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(tripExpenseService);
    }

    @Test
    void shouldRejectUnknownCategory() throws Exception {
        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "category": "FUEL", "amount": 10 }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tripExpenseService);
    }

    @Test
    void shouldAcceptExpenseWithoutDescription() throws Exception {
        when(tripExpenseService.registerExpenseForOwner(
                5L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("13.90"), null
        )).thenReturn(expense(9L));

        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "category": "TOLL", "amount": 13.90 }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAcceptZeroAmountForExemptToll() throws Exception {
        when(tripExpenseService.registerExpenseForOwner(
                5L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("0"), null
        )).thenReturn(TripExpense.restore(
                9L, 5L, ExpenseCategory.TOLL, new BigDecimal("0.00"), null,
                Instant.parse("2026-09-14T12:00:00Z")
        ));

        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "category": "TOLL", "amount": 0 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("TOLL"))
                .andExpect(jsonPath("$.amount").value(0.0));
    }

    @Test
    void shouldReturnBadRequestWhenZeroAmountIsNotAllowedForCategory() throws Exception {
        when(tripExpenseService.registerExpenseForOwner(
                5L, CURRENT_USER.getId(), ExpenseCategory.FOOD, new BigDecimal("0"), null
        )).thenThrow(new InvalidTripExpenseException("Somente o pedágio pode ter valor zero (isento)."));

        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "category": "FOOD", "amount": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Somente o pedágio pode ter valor zero (isento)."));
    }

    @Test
    void shouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tripExpenseService);
    }

    @Test
    void shouldReturnBadRequestWhenTripIsPlanned() throws Exception {
        when(tripExpenseService.registerExpenseForOwner(
                5L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("13.90"), "Praça 2"
        )).thenThrow(new InvalidTripStatusException("Viagem planejada."));

        mockMvc.perform(post("/trips/5/expenses")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Viagem planejada."));
    }

    @Test
    void shouldReturnForbiddenForForeignTrip() throws Exception {
        when(tripExpenseService.findExpensesByTripIdForOwner(5L, CURRENT_USER.getId()))
                .thenThrow(new TripAccessDeniedException("negado"));

        mockMvc.perform(get("/trips/5/expenses").principal(AUTHENTICATION))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFindFuelEntries() throws Exception {
        when(tripExpenseService.findExpensesByTripIdForOwner(5L, CURRENT_USER.getId()))
                .thenReturn(List.of(expense(1L), expense(2L)));

        mockMvc.perform(get("/trips/5/expenses").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void shouldUpdateExpense() throws Exception {
        when(tripExpenseService.updateExpenseForOwner(
                5L, 9L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("13.90"), "Praça 2"
        )).thenReturn(expense(9L));

        mockMvc.perform(put("/trips/5/expenses/9")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingExpense() throws Exception {
        when(tripExpenseService.updateExpenseForOwner(
                5L, 9L, CURRENT_USER.getId(), ExpenseCategory.TOLL, new BigDecimal("13.90"), "Praça 2"
        )).thenThrow(new TripExpenseNotFoundException("Gasto não encontrado"));

        mockMvc.perform(put("/trips/5/expenses/9")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteExpense() throws Exception {
        mockMvc.perform(delete("/trips/5/expenses/9").principal(AUTHENTICATION))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tripExpenseService).deleteExpenseForOwner(5L, 9L, CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingExpense() throws Exception {
        doThrow(new TripExpenseNotFoundException("Gasto não encontrado"))
                .when(tripExpenseService)
                .deleteExpenseForOwner(5L, 9L, CURRENT_USER.getId());

        mockMvc.perform(delete("/trips/5/expenses/9").principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }

    private TripExpense expense(long id) {
        return TripExpense.restore(
                id,
                5L,
                ExpenseCategory.TOLL,
                new BigDecimal("13.90"),
                "Praça 2",
                Instant.parse("2026-09-14T12:00:00Z")
        );
    }
}
