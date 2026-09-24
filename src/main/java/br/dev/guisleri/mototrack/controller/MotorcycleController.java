package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.CreateMotorcycleRequestDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/motorcycles")
public class MotorcycleController {

    private final MotorcycleService motorcycleService;
    private final UserService userService;

    public MotorcycleController(MotorcycleService motorcycleService, UserService userService) {
        this.motorcycleService = motorcycleService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<MotorcycleResponseDTO> createMotorcycle(
            @Valid @RequestBody CreateMotorcycleRequestDTO createMotorcycleRequest,
            Authentication authentication
    ) {
        User owner = userService.findUserByEmail(
                authentication.getName()
        );

        Motorcycle motorcycle = motorcycleService.registerMotorcycle(
                createMotorcycleRequest.brand(),
                createMotorcycleRequest.model(),
                createMotorcycleRequest.color(),
                createMotorcycleRequest.year(),
                createMotorcycleRequest.engineCapacity(),
                owner
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MotorcycleResponseDTO.from(motorcycle));
    }

    @GetMapping
    public List<MotorcycleResponseDTO> findCurrentUserMotorcycles(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User currentUser = userService.findUserByEmail(email);

        return motorcycleService.findMotorcyclesByOwnerId(currentUser.getId())
                .stream()
                .map(MotorcycleResponseDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleResponseDTO> findMotorcycleById(
            @PathVariable("id") Long motorcycleId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(
                authentication.getName()
        );

        Motorcycle motorcycle = motorcycleService.findMotorcycleByIdForOwner(
                motorcycleId,
                currentUser.getId()
        );

        return ResponseEntity.ok(
                MotorcycleResponseDTO.from(motorcycle)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMotorcycleById(
            @PathVariable("id") Long motorcycleId,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(
                authentication.getName()
        );

        motorcycleService.deleteMotorcycleByIdForOwner(
                motorcycleId,
                currentUser.getId()
        );

        return ResponseEntity.noContent().build();
    }

}
