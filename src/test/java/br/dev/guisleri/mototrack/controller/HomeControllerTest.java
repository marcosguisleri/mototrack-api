package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.HomeResponseDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.NextTripResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.dto.UserResponseDTO;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.HomeService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

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
    private HomeService homeService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldReturnCompleteHomeData() throws Exception {
        MotorcycleResponseDTO motorcycle = new MotorcycleResponseDTO(
                1L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                UserResponseDTO.from(CURRENT_USER)
        );
        NextTripResponseDTO nextTrip = new NextTripResponseDTO(
                10L,
                "Florianopolis",
                "Ushuaia",
                5432.1,
                TerrainType.MIXED,
                LocalDate.of(2026, 10, 5),
                8L,
                motorcycle
        );
        TripSummaryResponseDTO lastCompletedTrip = new TripSummaryResponseDTO(
                11L,
                "Araras",
                "Campinas",
                120.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 20),
                motorcycle
        );
        HomeResponseDTO home = new HomeResponseDTO(
                nextTrip,
                lastCompletedTrip,
                1_250.5,
                4L,
                2L
        );
        when(homeService.getHome(CURRENT_USER.getId())).thenReturn(home);

        mockMvc.perform(get("/home").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextTrip.id").value(10))
                .andExpect(jsonPath("$.nextTrip.destination").value("Ushuaia"))
                .andExpect(jsonPath("$.nextTrip.tripDate").value("2026-10-05"))
                .andExpect(jsonPath("$.nextTrip.terrain").value("MIXED"))
                .andExpect(jsonPath("$.nextTrip.daysUntil").value(8))
                .andExpect(jsonPath("$.nextTrip.motorcycle.id").value(1))
                .andExpect(jsonPath("$.lastCompletedTrip.id").value(11))
                .andExpect(jsonPath("$.lastCompletedTrip.destination")
                        .value("Campinas"))
                .andExpect(jsonPath("$.lastCompletedTrip.tripDate")
                        .value("2026-09-20"))
                .andExpect(jsonPath("$.lastCompletedTrip.terrain")
                        .value("ASPHALT"))
                .andExpect(jsonPath("$.lastCompletedTrip.motorcycle.id").value(1))
                .andExpect(jsonPath("$.totalCompletedDistanceKm").value(1_250.5))
                .andExpect(jsonPath("$.completedTrips").value(4))
                .andExpect(jsonPath("$.motorcycleCount").value(2));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(homeService).getHome(CURRENT_USER.getId());
    }

    @Test
    void shouldReturnHomeDataWithNullNextTripAndLastCompletedTrip() throws Exception {
        HomeResponseDTO home = new HomeResponseDTO(
                null,
                null,
                0.0,
                0L,
                0L
        );
        when(homeService.getHome(CURRENT_USER.getId())).thenReturn(home);

        mockMvc.perform(get("/home").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextTrip").value((Object) null))
                .andExpect(jsonPath("$.lastCompletedTrip").value((Object) null))
                .andExpect(jsonPath("$.totalCompletedDistanceKm").value(0.0))
                .andExpect(jsonPath("$.completedTrips").value(0))
                .andExpect(jsonPath("$.motorcycleCount").value(0));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(homeService).getHome(CURRENT_USER.getId());
    }
}
