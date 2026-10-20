package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.config.ClockConfig;
import br.dev.guisleri.mototrack.config.SecurityConfig;
import br.dev.guisleri.mototrack.controller.AuthController;
import br.dev.guisleri.mototrack.model.RefreshToken;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.RefreshTokenRepository;
import br.dev.guisleri.mototrack.repository.UserRepository;
import br.dev.guisleri.mototrack.service.AuthService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
    private static final String HASHED_REFRESH_TOKEN = "hashed-refresh-token";
    private static final String NEW_RAW_REFRESH_TOKEN = "new-raw-refresh-token";
    private static final String NEW_HASHED_REFRESH_TOKEN = "new-hashed-refresh-token";
    private static final User USER = User.restore(
            1L,
            "Marcos",
            EMAIL,
            new BCryptPasswordEncoder().encode(PASSWORD)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Autowired
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private RefreshTokenGenerator refreshTokenGenerator;

    // -------------------------------------------------------------------------
    // POST /auth/login - autenticação e emissão inicial de tokens
    // -------------------------------------------------------------------------

    @Test
    void shouldNormalizeEmailCaseWhenLoggingInWithCorrectPassword() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));
        when(refreshTokenGenerator.generate()).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("Marcos@Example.COM", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Jwt accessToken = jwtDecoder.decode(JsonPath.read(responseBody, "$.accessToken"));
        assertEquals(EMAIL, accessToken.getSubject());
        assertEquals("HS256", accessToken.getHeaders().get("alg"));
        assertEquals(Duration.ofMinutes(15),
                Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        assertFalse(accessToken.getClaims().containsKey("password"));
        assertFalse(accessToken.getClaims().containsKey("passwordHash"));
        assertFalse(accessToken.getClaims().containsKey("refreshToken"));

        verify(userRepository, times(2)).findByEmail(EMAIL);
    }

    @Test
    void shouldPersistRefreshTokenWhenLoginSucceeds() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));
        when(refreshTokenGenerator.generate()).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);

        Instant requestStartedAt = Instant.now(clock);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(RAW_REFRESH_TOKEN))
                .andExpect(content().string(not(containsString(HASHED_REFRESH_TOKEN))));

        Instant requestFinishedAt = Instant.now(clock);
        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenGenerator).generate();
        verify(refreshTokenGenerator).hash(RAW_REFRESH_TOKEN);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertEquals(USER.getId(), savedRefreshToken.getUserId());
        assertEquals(HASHED_REFRESH_TOKEN, savedRefreshToken.getTokenHash());
        assertFalse(savedRefreshToken.getExpiresAt().isBefore(
                requestStartedAt.plus(30, ChronoUnit.DAYS)
        ));
        assertFalse(savedRefreshToken.getExpiresAt().isAfter(
                requestFinishedAt.plus(30, ChronoUnit.DAYS)
        ));
    }

    @Test
    void shouldRejectIncorrectPasswordAtLogin() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(USER));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "incorrect-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userRepository).findByEmail(EMAIL);
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // -------------------------------------------------------------------------
    // POST /auth/refresh - rotação e validação de refresh tokens
    // -------------------------------------------------------------------------

    @Test
    void shouldRotateRefreshTokenWhenRefreshIsValid() throws Exception {
        RefreshToken existingRefreshToken = RefreshToken.restore(
                42L,
                USER.getId(),
                HASHED_REFRESH_TOKEN,
                Instant.now(clock).plus(30, ChronoUnit.DAYS)
        );
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.of(existingRefreshToken));
        when(userRepository.findById(USER.getId())).thenReturn(Optional.of(USER));
        when(refreshTokenGenerator.generate()).thenReturn(NEW_RAW_REFRESH_TOKEN);
        when(refreshTokenGenerator.hash(NEW_RAW_REFRESH_TOKEN))
                .thenReturn(NEW_HASHED_REFRESH_TOKEN);

        Instant requestStartedAt = Instant.now(clock);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(NEW_RAW_REFRESH_TOKEN));

        Instant requestFinishedAt = Instant.now(clock);
        ArgumentCaptor<RefreshToken> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenGenerator).hash(RAW_REFRESH_TOKEN);
        verify(refreshTokenRepository).findByTokenHashForUpdate(HASHED_REFRESH_TOKEN);
        verify(userRepository).findById(USER.getId());
        verify(refreshTokenRepository).deleteById(42L);
        verify(refreshTokenGenerator).generate();
        verify(refreshTokenGenerator).hash(NEW_RAW_REFRESH_TOKEN);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertEquals(USER.getId(), savedRefreshToken.getUserId());
        assertEquals(NEW_HASHED_REFRESH_TOKEN, savedRefreshToken.getTokenHash());
        assertFalse(savedRefreshToken.getExpiresAt().isBefore(
                requestStartedAt.plus(30, ChronoUnit.DAYS)
        ));
        assertFalse(savedRefreshToken.getExpiresAt().isAfter(
                requestFinishedAt.plus(30, ChronoUnit.DAYS)
        ));
    }

    @Test
    void shouldRejectUnknownRefreshToken() throws Exception {
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token inválido."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(refreshTokenRepository).findByTokenHashForUpdate(HASHED_REFRESH_TOKEN);
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).deleteById(any());
    }

    @Test
    void shouldRejectExpiredRefreshToken() throws Exception {
        RefreshToken expiredRefreshToken = RefreshToken.restore(
                42L,
                USER.getId(),
                HASHED_REFRESH_TOKEN,
                Instant.now(clock).minus(1, ChronoUnit.DAYS)
        );
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.of(expiredRefreshToken));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token expirado."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(refreshTokenRepository).deleteById(42L);
        verify(userRepository, never()).findById(any());
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectRefreshTokenWhenUserDoesNotExist() throws Exception {
        RefreshToken existingRefreshToken = RefreshToken.restore(
                42L,
                USER.getId(),
                HASHED_REFRESH_TOKEN,
                Instant.now(clock).plus(30, ChronoUnit.DAYS)
        );
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.of(existingRefreshToken));
        when(userRepository.findById(USER.getId())).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token inválido."))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userRepository).findById(USER.getId());
        verify(refreshTokenGenerator, never()).generate();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(refreshTokenRepository).deleteById(42L);
    }

    @Test
    void shouldRejectRefreshWithoutRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message")
                        .value("A requisição contém campos inválidos."))
                .andExpect(jsonPath("$.errors.refreshToken")
                        .value("O refresh token é obrigatório."));

        verify(refreshTokenGenerator, never()).hash(any());
        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
    }

    @Test
    void shouldNotReturnRefreshTokenHashInRefreshResponse() throws Exception {
        RefreshToken existingRefreshToken = RefreshToken.restore(
                42L,
                USER.getId(),
                HASHED_REFRESH_TOKEN,
                Instant.now(clock).plus(30, ChronoUnit.DAYS)
        );
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.of(existingRefreshToken));
        when(userRepository.findById(USER.getId())).thenReturn(Optional.of(USER));
        when(refreshTokenGenerator.generate()).thenReturn(NEW_RAW_REFRESH_TOKEN);
        when(refreshTokenGenerator.hash(NEW_RAW_REFRESH_TOKEN))
                .thenReturn(NEW_HASHED_REFRESH_TOKEN);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value(NEW_RAW_REFRESH_TOKEN))
                .andExpect(content().string(not(containsString(NEW_HASHED_REFRESH_TOKEN))));
    }

    // -------------------------------------------------------------------------
    // POST /auth/logout - revogação idempotente do refresh token
    // -------------------------------------------------------------------------

    @Test
    void shouldLogoutExistingRefreshToken() throws Exception {
        RefreshToken existingRefreshToken = RefreshToken.restore(
                42L,
                USER.getId(),
                HASHED_REFRESH_TOKEN,
                Instant.now(clock).plus(30, ChronoUnit.DAYS)
        );
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.of(existingRefreshToken));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenRepository).findByTokenHashForUpdate(HASHED_REFRESH_TOKEN);
        verify(refreshTokenRepository).deleteById(42L);
    }

    @Test
    void shouldReturnNoContentWhenLoggingOutUnknownRefreshToken() throws Exception {
        when(refreshTokenGenerator.hash(RAW_REFRESH_TOKEN))
                .thenReturn(HASHED_REFRESH_TOKEN);
        when(refreshTokenRepository.findByTokenHashForUpdate(HASHED_REFRESH_TOKEN))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenJson(RAW_REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenRepository).findByTokenHashForUpdate(HASHED_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).deleteById(any());
    }

    @Test
    void shouldRejectLogoutWithoutRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message")
                        .value("A requisição contém campos inválidos."))
                .andExpect(jsonPath("$.errors.refreshToken")
                        .value("O refresh token é obrigatório."));

        verify(refreshTokenGenerator, never()).hash(any());
        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
    }

    // -------------------------------------------------------------------------
    // Helpers de payload
    // -------------------------------------------------------------------------

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String refreshTokenJson(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }
}
