package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.UserService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String PASSWORD = "secret123";
    private static final User USER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com",
            "password-hash"
    );
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(
                    USER.getEmail(),
                    null,
                    List.of()
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUserWithoutExposingCredentials() throws Exception {
        when(userService.registerUser("Marcos", "marcos@example.com", PASSWORD))
                .thenReturn(USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "marcos@example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Marcos"))
                .andExpect(jsonPath("$.email").value("marcos@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).registerUser("Marcos", "marcos@example.com", PASSWORD);
    }

    @Test
    void shouldTrimEmailButPreservePassword() throws Exception {
        String passwordWithSpaces = "  secret123  ";
        when(userService.registerUser(
                "Marcos",
                "Marcos@Example.COM",
                passwordWithSpaces
        )).thenReturn(USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(
                                "Marcos",
                                "  Marcos@Example.COM  ",
                                passwordWithSpaces
                        )))
                .andExpect(status().isCreated());

        verify(userService).registerUser(
                "Marcos",
                "Marcos@Example.COM",
                passwordWithSpaces
        );
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "email-invalido", PASSWORD)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(" ", "marcos@example.com", PASSWORD)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectShortPassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "marcos@example.com", "short")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String message = "Já existe um usuário cadastrado com este e-mail.";
        when(userService.registerUser("Marcos", "marcos@example.com", PASSWORD))
                .thenThrow(new UserAlreadyExistsException(message));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Marcos", "marcos@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(content().string(message));
    }

    @Test
    void shouldReturnCurrentAuthenticatedUserWithoutExposingCredentials() throws Exception {
        when(userService.findUserByEmail(USER.getEmail())).thenReturn(USER);

        mockMvc.perform(get("/users/me").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Marcos"))
                .andExpect(jsonPath("$.email").value("marcos@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).findUserByEmail(USER.getEmail());
    }

    @Test
    void shouldNotExposeUserCollectionEndpoint() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldNotExposeUserByIdEndpoint() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(userService);
    }

    private String userJson(String name, String email, String password) {
        return """
                {
                  "name": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(name, email, password);
    }
}
