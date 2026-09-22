package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.CreateMotorcycleRequestDTO;
import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/motorcycles")
public class MotorcycleController {

    private final MotorcycleService motorcycleService;

    public MotorcycleController(MotorcycleService motorcycleService) {
        this.motorcycleService = motorcycleService;
    }

    @PostMapping
    public ResponseEntity<MotorcycleResponseDTO> createMotorcycle(
            @Valid @RequestBody CreateMotorcycleRequestDTO createMotorcycleRequest
    ) {
        Motorcycle motorcycle = motorcycleService.registerMotorcycle(
                createMotorcycleRequest.brand(),
                createMotorcycleRequest.model(),
                createMotorcycleRequest.color(),
                createMotorcycleRequest.year(),
                createMotorcycleRequest.engineCapacity(),
                createMotorcycleRequest.ownerId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MotorcycleResponseDTO.from(motorcycle));
    }

    @GetMapping
    public List<MotorcycleResponseDTO> findAllMotorcycles() {
        return motorcycleService.findAllMotorcycles()
                .stream()
                .map(MotorcycleResponseDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleResponseDTO> findMotorcycleById(
            @PathVariable("id") Long motorcycleId
    ) {
        Motorcycle motorcycle = motorcycleService.findMotorcycleById(motorcycleId);

        return ResponseEntity.ok(MotorcycleResponseDTO.from(motorcycle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMotorcycleById(
            @PathVariable("id") Long motorcycleId
    ) {
        motorcycleService.deleteMotorcycleById(motorcycleId);

        return ResponseEntity.noContent().build();
    }

}
