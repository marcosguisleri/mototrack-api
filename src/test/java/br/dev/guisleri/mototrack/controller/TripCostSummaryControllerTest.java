package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;
import br.dev.guisleri.mototrack.model.TripCostSummary;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripCostSummaryService;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripCostSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripCostSummaryControllerTest {

    private static final User CURRENT_USER = User.restore(
            1L, "Marcos", "marcos@example.com", "password-hash"
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    CURRENT_USER.getEmail(), null, List.of()
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripCostSummaryService tripCostSummaryService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldReturnCostSummary() throws Exception {
        when(tripCostSummaryService.getCostSummaryForOwner(5L, CURRENT_USER.getId()))
                .thenReturn(TripCostSummary.of(
                        new FuelCostSummary(
                                2, new BigDecimal("16.2"), new BigDecimal("103.90")
                        ),
                        List.of(new ExpenseCategorySummary(
                                ExpenseCategory.TOLL, 3, new BigDecimal("37.80")
                        )),
                        100.0
                ));

        mockMvc.perform(get("/trips/5/cost-summary").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuel.entries").value(2))
                .andExpect(jsonPath("$.fuel.liters").value(16.2))
                .andExpect(jsonPath("$.fuel.totalCost").value(103.90))
                .andExpect(jsonPath("$.expenses.length()").value(1))
                .andExpect(jsonPath("$.expenses[0].category").value("TOLL"))
                .andExpect(jsonPath("$.expenses[0].count").value(3))
                .andExpect(jsonPath("$.expenses[0].total").value(37.80))
                .andExpect(jsonPath("$.totalCost").value(141.70))
                .andExpect(jsonPath("$.costPerKm").value(1.42));
    }

    @Test
    void shouldReturnNullTotalsWhenNothingWasRecorded() throws Exception {
        when(tripCostSummaryService.getCostSummaryForOwner(5L, CURRENT_USER.getId()))
                .thenReturn(TripCostSummary.of(
                        new FuelCostSummary(0, null, null), List.of(), 100.0
                ));

        mockMvc.perform(get("/trips/5/cost-summary").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuel.entries").value(0))
                .andExpect(jsonPath("$.fuel.liters").isEmpty())
                .andExpect(jsonPath("$.fuel.totalCost").isEmpty())
                .andExpect(jsonPath("$.expenses.length()").value(0))
                .andExpect(jsonPath("$.totalCost").isEmpty())
                .andExpect(jsonPath("$.costPerKm").isEmpty());
    }

    @Test
    void shouldReturnForbiddenForForeignTrip() throws Exception {
        when(tripCostSummaryService.getCostSummaryForOwner(5L, CURRENT_USER.getId()))
                .thenThrow(new TripAccessDeniedException("negado"));

        mockMvc.perform(get("/trips/5/cost-summary").principal(AUTHENTICATION))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundForUnknownTrip() throws Exception {
        when(tripCostSummaryService.getCostSummaryForOwner(5L, CURRENT_USER.getId()))
                .thenThrow(new TripNotFoundException("não encontrada"));

        mockMvc.perform(get("/trips/5/cost-summary").principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }
}
