package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TripStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTripStatusRequestDTO(
        @NotNull(message = "O status da viagem é obrigatório.")
        TripStatus status
) {
}
