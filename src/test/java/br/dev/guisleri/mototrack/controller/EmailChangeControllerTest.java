package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.exception.InvalidCurrentPasswordException;
import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.VerificationCodeCooldownException;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.EmailChangeService;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailChangeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailChangeControllerTest {

    private static final User USER = User.restore(1L, "Marcos", "marcos@example.com", "hash");
    private static final Authentication AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(USER.getEmail(), null, List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailChangeService emailChangeService;

    @MockitoBean
    private UserService userService;

    // -------------------------------------------------------------------------
    // POST /users/me/email-change
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptTheRequestForTheAuthenticatedUser() throws Exception {
        when(userService.findUserByEmail("marcos@example.com")).thenReturn(USER);

        mockMvc.perform(post("/users/me/email-change")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("  novo@example.com ", "secret123")))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(emailChangeService).requestChange(USER, "novo@example.com", "secret123");
    }

    @Test
    void shouldAnswer400ForAWrongCurrentPassword() throws Exception {
        when(userService.findUserByEmail("marcos@example.com")).thenReturn(USER);
        doThrow(new InvalidCurrentPasswordException("Senha atual incorreta."))
                .when(emailChangeService).requestChange(USER, "novo@example.com", "wrong");

        mockMvc.perform(post("/users/me/email-change")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("novo@example.com", "wrong")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Senha atual incorreta."));
    }

    @Test
    void shouldAnswer409WhenTheAddressIsTaken() throws Exception {
        when(userService.findUserByEmail("marcos@example.com")).thenReturn(USER);
        doThrow(new UserAlreadyExistsException("Já existe um usuário cadastrado com este e-mail."))
                .when(emailChangeService).requestChange(USER, "outro@example.com", "secret123");

        mockMvc.perform(post("/users/me/email-change")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("outro@example.com", "secret123")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAnswer429WhenAskedAgainTooSoon() throws Exception {
        when(userService.findUserByEmail("marcos@example.com")).thenReturn(USER);
        doThrow(new VerificationCodeCooldownException("Aguarde um minuto antes de pedir outro código."))
                .when(emailChangeService).requestChange(USER, "novo@example.com", "secret123");

        mockMvc.perform(post("/users/me/email-change")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("novo@example.com", "secret123")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("Aguarde um minuto antes de pedir outro código."));
    }

    @Test
    void shouldRejectAnInvalidNewEmailOrAMissingPassword() throws Exception {
        mockMvc.perform(post("/users/me/email-change")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newEmail").exists())
                .andExpect(jsonPath("$.errors.currentPassword").exists());

        verifyNoInteractions(emailChangeService);
    }

    // -------------------------------------------------------------------------
    // POST /users/me/email-change/confirm
    // -------------------------------------------------------------------------

    @Test
    void shouldConfirmAndReturnTheNewSessionTokens() throws Exception {
        when(userService.findUserByEmail("marcos@example.com")).thenReturn(USER);
        when(emailChangeService.confirmChange(1L, "654321"))
                .thenReturn(new AuthTokenResponseDTO("new-access", "new-refresh"));

        mockMvc.perform(post("/users/me/email-change/confirm")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void shouldRejectACodeThatIsNotSixDigits() throws Exception {
        mockMvc.perform(post("/users/me/email-change/confirm")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").value("O código deve ter 6 dígitos."));

        verifyNoInteractions(emailChangeService);
    }

    private String requestJson(String newEmail, String password) {
        return """
                {"newEmail":"%s","currentPassword":"%s"}
                """.formatted(newEmail, password);
    }
}
