package br.dev.guisleri.mototrack.dto;

import java.time.Instant;

public record UpdateTripTimestampsRequestDTO(
        Instant startedAt,
        Instant completedAt
) {
}
