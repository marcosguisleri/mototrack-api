package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TripStatus;

public record ChangeTripStatusRequestDTO(
        TripStatus status
) {
}
