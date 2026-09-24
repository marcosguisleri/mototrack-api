package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.MotorcycleAccessDeniedException;
import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleService;
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

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MotorcycleController.class)
@AutoConfigureMockMvc(addFilters = false)
class MotorcycleControllerTest {

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
    private MotorcycleService motorcycleService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(userService.findUserByEmail(CURRENT_USER.getEmail()))
                .thenReturn(CURRENT_USER);
    }

    @Test
    void shouldCreateMotorcycleForAuthenticatedUser() throws Exception {
        Motorcycle savedMotorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleService.registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                CURRENT_USER
        )).thenReturn(savedMotorcycle);

        mockMvc.perform(post("/motorcycles")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createMotorcycleJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("NX 500"))
                .andExpect(jsonPath("$.color").value("Black"))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.engineCapacity").value(471))
                .andExpect(jsonPath("$.owner.id").value(1))
                .andExpect(jsonPath("$.owner.name").value("Marcos"))
                .andExpect(jsonPath("$.owner.email").value("marcos@example.com"));

        verify(userService).findUserByEmail(CURRENT_USER.getEmail());
        verify(motorcycleService).registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                CURRENT_USER
        );
    }

    @Test
    void shouldNotAllowRequestToChooseMotorcycleOwner() throws Exception {
        Motorcycle savedMotorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleService.registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                CURRENT_USER
        )).thenReturn(savedMotorcycle);

        mockMvc.perform(post("/motorcycles")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createMotorcycleJsonWithOwnerId(999L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.id").value(CURRENT_USER.getId()));

        verify(motorcycleService).registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                CURRENT_USER
        );
    }

    @Test
    void shouldReturnFriendlyMessagesForInvalidMotorcycle() throws Exception {
        mockMvc.perform(post("/motorcycles")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": " ",
                                  "model": "",
                                  "color": " ",
                                  "year": 1899,
                                  "engineCapacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.brand").value("A marca é obrigatória."))
                .andExpect(jsonPath("$.errors.model").value("O modelo é obrigatório."))
                .andExpect(jsonPath("$.errors.color").value("A cor é obrigatória."))
                .andExpect(jsonPath("$.errors.year")
                        .value("O ano deve ser igual ou posterior a 1900."))
                .andExpect(jsonPath("$.errors.engineCapacity")
                        .value("A cilindrada deve ser maior que zero."));

        verifyNoInteractions(motorcycleService);
    }

    @Test
    void shouldListOnlyAuthenticatedUsersMotorcycles() throws Exception {
        Motorcycle honda = motorcycle(1L, "Honda", "NX 500");
        Motorcycle yamaha = motorcycle(2L, "Yamaha", "Tenere 700");
        when(motorcycleService.findMotorcyclesByOwnerId(CURRENT_USER.getId()))
                .thenReturn(List.of(honda, yamaha));

        mockMvc.perform(get("/motorcycles").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].owner.id").value(CURRENT_USER.getId()))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].owner.id").value(CURRENT_USER.getId()));

        verify(motorcycleService).findMotorcyclesByOwnerId(CURRENT_USER.getId());
    }

    @Test
    void shouldFindAuthenticatedUsersMotorcycleById() throws Exception {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleService.findMotorcycleByIdForOwner(1L, CURRENT_USER.getId()))
                .thenReturn(motorcycle);

        mockMvc.perform(get("/motorcycles/1").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner.id").value(CURRENT_USER.getId()));

        verify(motorcycleService).findMotorcycleByIdForOwner(1L, CURRENT_USER.getId());
    }

    @Test
    void shouldReturnNotFoundWhenMotorcycleDoesNotExist() throws Exception {
        when(motorcycleService.findMotorcycleByIdForOwner(999L, CURRENT_USER.getId()))
                .thenThrow(new MotorcycleNotFoundException(
                        "Motocicleta com id 999 não encontrada"
                ));

        mockMvc.perform(get("/motorcycles/999").principal(AUTHENTICATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Motocicleta com id 999 não encontrada"));
    }

    @Test
    void shouldReturnForbiddenWhenMotorcycleBelongsToAnotherUser() throws Exception {
        String message = "Você não possui permissão para acessar esta motocicleta.";
        when(motorcycleService.findMotorcycleByIdForOwner(1L, CURRENT_USER.getId()))
                .thenThrow(new MotorcycleAccessDeniedException(message));

        mockMvc.perform(get("/motorcycles/1").principal(AUTHENTICATION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void shouldDeleteAuthenticatedUsersMotorcycleWithoutTrips() throws Exception {
        mockMvc.perform(delete("/motorcycles/1").principal(AUTHENTICATION))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(motorcycleService).deleteMotorcycleByIdForOwner(
                1L,
                CURRENT_USER.getId()
        );
    }

    @Test
    void shouldReturnForbiddenWhenDeletingAnotherUsersMotorcycle() throws Exception {
        String message = "Você não possui permissão para acessar esta motocicleta.";
        doThrow(new MotorcycleAccessDeniedException(message))
                .when(motorcycleService)
                .deleteMotorcycleByIdForOwner(1L, CURRENT_USER.getId());

        mockMvc.perform(delete("/motorcycles/1").principal(AUTHENTICATION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void shouldReturnConflictWhenDeletingOwnersMotorcycleWithTrips() throws Exception {
        String message =
                "A motocicleta não pode ser excluída enquanto possuir viagens associadas.";
        doThrow(new MotorcycleInUseException(message))
                .when(motorcycleService)
                .deleteMotorcycleByIdForOwner(1L, CURRENT_USER.getId());

        mockMvc.perform(delete("/motorcycles/1").principal(AUTHENTICATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingUnknownMotorcycle() throws Exception {
        String message = "Motocicleta com id 999 não encontrada";
        doThrow(new MotorcycleNotFoundException(message))
                .when(motorcycleService)
                .deleteMotorcycleByIdForOwner(999L, CURRENT_USER.getId());

        mockMvc.perform(delete("/motorcycles/999").principal(AUTHENTICATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(message));
    }

    private Motorcycle motorcycle(Long id, String brand, String model) {
        return Motorcycle.restore(
                id,
                brand,
                model,
                "Black",
                2025,
                471,
                CURRENT_USER
        );
    }

    private String createMotorcycleJson() {
        return """
                {
                  "brand": "Honda",
                  "model": "NX 500",
                  "color": "Black",
                  "year": 2025,
                  "engineCapacity": 471
                }
                """;
    }

    private String createMotorcycleJsonWithOwnerId(long ownerId) {
        return """
                {
                  "brand": "Honda",
                  "model": "NX 500",
                  "color": "Black",
                  "year": 2025,
                  "engineCapacity": 471,
                  "ownerId": %d
                }
                """.formatted(ownerId);
    }
}
