package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.config.SecurityConfig;
import br.dev.guisleri.mototrack.controller.UserController;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.UserRepository;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({SecurityConfig.class, CustomUserDetailsService.class})
class UserSecurityTest {

    private static final String EMAIL = "marcos@example.com";
    private static final String PASSWORD = "correct-password";
    private static final User USER = User.restore(
            1L,
            "Marcos",
            EMAIL,
            new BCryptPasswordEncoder().encode(PASSWORD)
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldAllowPublicUserRegistration() throws Exception {
        when(userService.registerUser("Marcos", EMAIL, PASSWORD))
                .thenReturn(USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).registerUser("Marcos", EMAIL, PASSWORD);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, userRepository);
    }

    @Test
    void shouldAuthenticateCorrectPasswordUsingNormalizedEmail() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));
        when(userService.findUserByEmail(EMAIL)).thenReturn(USER);

        mockMvc.perform(get("/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuthorization("  Marcos@Example.COM  ", PASSWORD)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userRepository).findByEmail(EMAIL);
        verify(userService).findUserByEmail(EMAIL);
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));

        mockMvc.perform(get("/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuthorization(EMAIL, "incorrect-password")
                        ))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findByEmail(EMAIL);
        verifyNoInteractions(userService);
    }

    private String basicAuthorization(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encoded;
    }

    private String userJson() {
        return """
                {
                  "name": "Marcos",
                  "email": "marcos@example.com",
                  "password": "correct-password"
                }
                """;
    }
}
