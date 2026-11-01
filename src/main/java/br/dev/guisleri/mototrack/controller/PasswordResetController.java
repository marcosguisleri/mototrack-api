package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.ForgotPasswordRequestDTO;
import br.dev.guisleri.mototrack.dto.ResetPasswordRequestDTO;
import br.dev.guisleri.mototrack.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request
    ) {
        passwordResetService.requestReset(request.email());

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        passwordResetService.resetPassword(
                request.email(),
                request.code(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }

}
