package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.AuthTokenResponseDTO;
import br.dev.guisleri.mototrack.dto.LoginRequestDTO;
import br.dev.guisleri.mototrack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDTO> authenticate(
            @Valid @RequestBody LoginRequestDTO authenticationRequestDTO
    ) {
        AuthTokenResponseDTO response =
                authService.login(authenticationRequestDTO);

        return ResponseEntity.ok(response);
    }

}
