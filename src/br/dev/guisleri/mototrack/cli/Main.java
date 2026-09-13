package br.dev.guisleri.mototrack.cli;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.TripService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Main {

    void main() {
        TripService tripService = new TripService();
        LocalDate today = LocalDate.now();

        // =========================
        // Cadastro das viagens
        // =========================

        tripService.addTrip(
                new Trip(
                        1,
                        "Serra do Rio do Rastro",
                        284.5,
                        TerrainType.ASPHALT,
                        today.plusDays(30)
                )
        );

        tripService.addTrip(
                new Trip(
                        2,
                        "Estrada Real",
                        710.0,
                        TerrainType.MIXED,
                        today.plusDays(90)
                )
        );

        tripService.addTrip(
                new Trip(
                        3,
                        "Jalapao",
                        920.0,
                        TerrainType.OFF_ROAD,
                        today.plusDays(1)
                )
        );

        tripService.addTrip(
                new Trip(
                        4,
                        "Serra Negra",
                        100.0,
                        TerrainType.ASPHALT,
                        today.plusDays(7)
                )
        );

        // =========================
        // Busca por ID
        // =========================

        IO.println("=== Busca por id ===");

        tripService.findTripById(2)
                .ifPresentOrElse(
                        this::printTrip,
                        () -> IO.println("Viagem nao encontrada")
                );

        IO.println(
                "Busca pelo id 99 esta vazia: "
                        + tripService.findTripById(99).isEmpty()
        );

        // =========================
        // Alteração de status
        // =========================

        IO.println("\n=== Alteracao de status ===");

        tripService.changeStatus(2, TripStatus.IN_PROGRESS);
        tripService.changeStatus(3, TripStatus.COMPLETED);

        tripService.findTripById(2)
                .ifPresent(this::printTrip);

        tripService.findTripById(3)
                .ifPresent(this::printTrip);

        // =========================
        // Busca por terreno
        // =========================

        IO.println("\n=== Viagens por terreno: ASPHALT ===");

        printTrips(
                tripService.findTripsByTerrain(TerrainType.ASPHALT)
        );

        IO.println("\n=== Viagens por terreno: OFF_ROAD ===");

        printTrips(
                tripService.findTripsByTerrain(TerrainType.OFF_ROAD)
        );

        // =========================
        // Viagens planejadas
        // =========================

        IO.println("\n=== Viagens planejadas ===");

        printTrips(
                tripService.findPlannedTrips()
        );

        // =========================
        // Contagem por status
        // =========================

        IO.println("\n=== Quantidade por status ===");

        printCountByStatus(
                tripService.countByStatus()
        );

        // =========================
        // Busca por data
        // =========================

        LocalDate searchedDate = today.plusDays(7);

        IO.println(
                "\n=== Viagens para " + searchedDate + " ==="
        );

        printTrips(
                tripService.findTripsByDate(searchedDate)
        );

        // =========================
        // Próximas viagens
        // =========================

        IO.println("\n=== Proximas viagens ===");

        printTrips(
                tripService.findUpcomingTrips()
        );

        // =========================
        // ID inexistente
        // =========================

        IO.println("\n=== Alteracao de status com id inexistente ===");

        try {
            tripService.changeStatus(
                    99,
                    TripStatus.COMPLETED
            );
        } catch (TripNotFoundException exception) {
            IO.println(exception.getMessage());
        }

        // =========================
        // Data inválida
        // =========================

        IO.println("\n=== Cadastro com data no passado ===");

        try {
            tripService.addTrip(
                    new Trip(
                            5,
                            "Viagem no tempo",
                            150.0,
                            TerrainType.ASPHALT,
                            today.minusDays(1)
                    )
            );
        } catch (InvalidTripDateException exception) {
            IO.println(exception.getMessage());
        }
    }

    private void printTrips(List<Trip> trips) {
        trips.forEach(this::printTrip);
    }

    private void printTrip(Trip trip) {
        System.out.printf(
                "Id: %d | Destino: %s | Distancia: %.1f km | Terreno: %s | Status: %s | Data: %s | Dias restantes: %d%n",
                trip.getId(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getStatus(),
                trip.getPlannedDate(),
                trip.getDaysUntilPlannedDate()
        );
    }

    private void printCountByStatus(
            Map<TripStatus, Long> countByStatus
    ) {
        for (TripStatus status : TripStatus.values()) {
            System.out.printf(
                    "%s: %d%n",
                    status,
                    countByStatus.getOrDefault(status, 0L)
            );
        }
    }
}