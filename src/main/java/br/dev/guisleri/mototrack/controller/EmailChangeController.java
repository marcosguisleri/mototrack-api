package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.dto.ChangeEmailRequestDTO;
import br.dev.guisleri.mototrack.dto.ConfirmEmailChangeRequestDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.EmailChangeService;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/email-change")
public class EmailChangeController {

    private final EmailChangeService emailChangeService;
    private final UserService userService;

    public EmailChangeController(
            EmailChangeService emailChangeService,
            UserService userService
    ) {
        this.emailChangeService = emailChangeService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Void> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequestDTO request,
            Authentication authentication
    ) {
        User user = userService.findUserByEmail(authentication.getName());

        emailChangeService.requestChange(
                user,
                request.newEmail(),
                request.currentPassword()
        );

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<AuthTokenResponseDTO> confirmEmailChange(
            @Valid @RequestBody ConfirmEmailChangeRequestDTO request,
            Authentication authentication
    ) {
        User user = userService.findUserByEmail(authentication.getName());

        return ResponseEntity.ok(
                emailChangeService.confirmChange(user.getId(), request.code())
        );
    }

}
