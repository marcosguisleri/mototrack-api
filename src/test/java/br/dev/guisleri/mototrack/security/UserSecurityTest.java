package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.config.SecurityConfig;
import br.dev.guisleri.mototrack.controller.UserController;
import br.dev.guisleri.mototrack.model.User;
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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import(SecurityConfig.class)
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

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserService userService;

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
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        startsWith("Bearer")
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Autenticação necessária ou credenciais inválidas."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldUseJwtSubjectAsAuthenticationNameWhenGettingCurrentUser() throws Exception {
        when(userService.findUserByEmail(EMAIL)).thenReturn(USER);

        mockMvc.perform(get("/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + validJwtWithSubject(EMAIL)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Marcos"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).findUserByEmail(EMAIL);
    }

    @Test
    void shouldReturnStandardNotFoundForAuthenticatedUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/unknown-endpoint")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + validJwtWithSubject(EMAIL)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Endpoint não encontrado."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verifyNoInteractions(userService);
    }

    private String validJwtWithSubject(String subject) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(15, ChronoUnit.MINUTES))
                .subject(subject)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
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
