package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.CreateUserRequestDTO;
import br.dev.guisleri.mototrack.dto.UserResponseDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserRequestDTO requestDTO
    ) {
        User user = userService.registerUser(
                requestDTO.name(),
                requestDTO.email(),
                requestDTO.password()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDTO.from(user));

    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userService.findUserByEmail(email);

        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

}
