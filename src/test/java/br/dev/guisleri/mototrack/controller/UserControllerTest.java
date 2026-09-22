package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final User USER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MotorcycleService motorcycleService;

    @Test
    void shouldCreateUser() throws Exception {
        when(userService.registerUser("Marcos", "marcos@example.com"))
                .thenReturn(USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "marcos@example.com")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Marcos"))
                .andExpect(jsonPath("$.email").value("marcos@example.com"));

        verify(userService).registerUser("Marcos", "marcos@example.com");
    }

    @Test
    void shouldCreateUserWithTrimmedEmail() throws Exception {
        when(userService.registerUser("Marcos", "Marcos@Example.COM"))
                .thenReturn(USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "  Marcos@Example.COM  ")))
                .andExpect(status().isCreated());

        verify(userService).registerUser("Marcos", "Marcos@Example.COM");
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "email-invalido")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(" ", "marcos@example.com")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String message = "Já existe um usuário cadastrado com este e-mail.";
        when(userService.registerUser("Marcos", "marcos@example.com"))
                .thenThrow(new UserAlreadyExistsException(message));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "marcos@example.com")))
                .andExpect(status().isConflict())
                .andExpect(content().string(message));
    }

    @Test
    void shouldFindAllUsers() throws Exception {
        User ana = User.restore(2L, "Ana", "ana@example.com");
        when(userService.findAllUsers()).thenReturn(List.of(USER, ana));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Marcos"))
                .andExpect(jsonPath("$[0].email").value("marcos@example.com"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Ana"));

        verify(userService).findAllUsers();
    }

    @Test
    void shouldFindUserById() throws Exception {
        when(userService.findUserById(1L)).thenReturn(USER);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Marcos"))
                .andExpect(jsonPath("$.email").value("marcos@example.com"));

        verify(userService).findUserById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        String message = "Usuário com id 999 não encontrado";
        when(userService.findUserById(999L))
                .thenThrow(new UserNotFoundException(message));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(message));
    }

    @Test
    void shouldFindMotorcyclesByUserId() throws Exception {
        User ana = User.restore(2L, "Ana", "ana@example.com");
        Motorcycle honda = Motorcycle.restore(
                1L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                USER
        );
        Motorcycle yamaha = Motorcycle.restore(
                2L,
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                USER
        );
        Motorcycle dafra = Motorcycle.restore(
                3L,
                "Dafra",
                "NH 300",
                "Red",
                2025,
                291,
                ana
        );
        when(motorcycleService.findMotorcyclesByOwnerId(1L))
                .thenReturn(List.of(honda, yamaha));
        when(motorcycleService.findMotorcyclesByOwnerId(2L))
                .thenReturn(List.of(dafra));

        mockMvc.perform(get("/users/1/motorcycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].brand").value("Honda"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].brand").value("Yamaha"))
                .andExpect(jsonPath("$[0].owner.id").value(1))
                .andExpect(jsonPath("$[0].owner.email").value("marcos@example.com"));

        mockMvc.perform(get("/users/2/motorcycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].brand").value("Dafra"))
                .andExpect(jsonPath("$[0].owner.id").value(2))
                .andExpect(jsonPath("$[0].owner.email").value("ana@example.com"));

        verify(motorcycleService).findMotorcyclesByOwnerId(1L);
        verify(motorcycleService).findMotorcyclesByOwnerId(2L);
    }

    @Test
    void shouldReturnNotFoundWhenFindingMotorcyclesForUnknownUser() throws Exception {
        String message = "Usuário com id 999 não encontrado";
        when(motorcycleService.findMotorcyclesByOwnerId(999L))
                .thenThrow(new UserNotFoundException(message));

        mockMvc.perform(get("/users/999/motorcycles"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(message));

        verify(motorcycleService).findMotorcyclesByOwnerId(999L);
    }

    private String userJson(String name, String email) {
        return """
                {
                  "name": "%s",
                  "email": "%s"
                }
                """.formatted(name, email);
    }
}
