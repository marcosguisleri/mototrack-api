package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.InvalidVerificationCodeException;
import br.dev.guisleri.mototrack.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    // -------------------------------------------------------------------------
    // POST /auth/forgot-password
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptTheRequestWithoutABody() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  marcos@example.com \"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(passwordResetService).requestReset("marcos@example.com");
    }

    @Test
    void shouldRejectAnInvalidEmailWithoutCallingTheService() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email")
                        .value("Informe um endereço de e-mail válido."));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void shouldRejectAMissingEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("O e-mail é obrigatório."));
    }

    // -------------------------------------------------------------------------
    // POST /auth/reset-password
    // -------------------------------------------------------------------------

    @Test
    void shouldResetThePassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson("marcos@example.com", "123456", "new-secret-1")))
                .andExpect(status().isNoContent());

        verify(passwordResetService)
                .resetPassword("marcos@example.com", "123456", "new-secret-1");
    }

    @Test
    void shouldAnswer400WithTheGenericMessageForABadCode() throws Exception {
        doThrow(new InvalidVerificationCodeException("Código inválido ou expirado."))
                .when(passwordResetService)
                .resetPassword("marcos@example.com", "000000", "new-secret-1");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson("marcos@example.com", "000000", "new-secret-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Código inválido ou expirado."));
    }

    @Test
    void shouldRejectACodeThatIsNotSixDigits() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson("marcos@example.com", "12ab", "new-secret-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").value("O código deve ter 6 dígitos."));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void shouldRejectAShortPassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson("marcos@example.com", "123456", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword")
                        .value("A senha deve ter no mínimo 8 caracteres."));

        verify(passwordResetService, org.mockito.Mockito.never())
                .resetPassword(any(), any(), any());
    }

    private String resetJson(String email, String code, String password) {
        return """
                {"email":"%s","code":"%s","newPassword":"%s"}
                """.formatted(email, code, password);
    }
}
