package br.dev.guisleri.mototrack.security;

import br.dev.guisleri.mototrack.config.SecurityConfig;
import br.dev.guisleri.mototrack.controller.EmailChangeController;
import br.dev.guisleri.mototrack.controller.PasswordResetController;
import br.dev.guisleri.mototrack.repository.UserRepository;
import br.dev.guisleri.mototrack.service.EmailChangeService;
import br.dev.guisleri.mototrack.service.PasswordResetService;
import br.dev.guisleri.mototrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PasswordResetController.class, EmailChangeController.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({SecurityConfig.class, CustomUserDetailsService.class})
class AccountRecoverySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private EmailChangeService emailChangeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Recuperar a senha é público: quem esqueceu a senha não tem token
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowForgotPasswordWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"marcos@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset("marcos@example.com");
    }

    @Test
    void shouldAllowResetPasswordWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"marcos@example.com","code":"123456","newPassword":"new-secret-1"}
                                """))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // Trocar o e-mail exige estar logado
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectEmailChangeRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/users/me/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\":\"novo@example.com\",\"currentPassword\":\"secret123\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailChangeService);
    }

    @Test
    void shouldRejectEmailChangeConfirmationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/users/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(emailChangeService);
    }
}
