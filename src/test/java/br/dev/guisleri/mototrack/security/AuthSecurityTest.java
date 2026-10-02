package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.config.ClockConfig;
import br.dev.guisleri.mototrack.config.SecurityConfig;
import br.dev.guisleri.mototrack.controller.AuthController;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.UserRepository;
import br.dev.guisleri.mototrack.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        ClockConfig.class,
        CustomUserDetailsService.class,
        AuthService.class
})
class AuthSecurityTest {

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
    private UserRepository userRepository;

    @Test
    void shouldNormalizeEmailCaseWhenLoggingInWithCorrectPassword() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("Marcos@Example.COM", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void shouldRejectIncorrectPasswordAtLogin() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "incorrect-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userRepository).findByEmail(EMAIL);
    }

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
