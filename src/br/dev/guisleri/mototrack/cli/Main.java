package br.dev.guisleri.mototrack.cli;

import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.TripService;

import java.util.List;
import java.util.Map;

public class Main {

    void main() {
        TripService tripService = new TripService();

        tripService.addTrip(new Trip(1, "Serra do Rio do Rastro", 284.5, TerrainType.ASPHALT));
        tripService.addTrip(new Trip(2, "Estrada Real", 710.0, TerrainType.MIXED));
        tripService.addTrip(new Trip(3, "Jalapao", 920.0, TerrainType.OFF_ROAD));
        tripService.addTrip(new Trip(4, "Serra Negra", 100.0, TerrainType.ASPHALT));

        IO.println("=== Busca por id ===");
        tripService.findTripById(2)
                .ifPresentOrElse(
                        this::printTrip,
                        () -> IO.println("Viagem nao encontrada")
                );
        IO.println("Busca pelo id 99: " + tripService.findTripById(99).isEmpty());

        IO.println("\n=== Alteracao de status ===");
        tripService.changeStatus(2, TripStatus.IN_PROGRESS);
        tripService.changeStatus(3, TripStatus.COMPLETED);
        tripService.findTripById(2).ifPresent(this::printTrip);
        tripService.findTripById(3).ifPresent(this::printTrip);

        IO.println("\n=== Viagens por terreno: OFF_ROAD ===");
        printTrips(tripService.findTripsByTerrain(TerrainType.OFF_ROAD));

        IO.println("\n=== Viagens planejadas ===");
        printTrips(tripService.findPlannedTrips());

        IO.println("\n=== Quantidade por status ===");
        printCountByStatus(tripService.countByStatus());

        IO.println("\n=== Id inexistente ao alterar status ===");
        try {
            tripService.changeStatus(99, TripStatus.COMPLETED);
        } catch (TripNotFoundException exception) {
            IO.println(exception.getMessage());
        }
    }

    private void printTrips(List<Trip> trips) {
        trips.forEach(this::printTrip);
    }

    private void printTrip(Trip trip) {
        System.out.printf(
                "Id: %d | Destino: %s | Distancia: %.1f km | Terreno: %s | Status: %s%n",
                trip.getId(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getStatus()
        );
    }

    private void printCountByStatus(Map<TripStatus, Long> countByStatus) {
        for (TripStatus status : TripStatus.values()) {
            System.out.printf("%s: %d%n", status, countByStatus.getOrDefault(status, 0L));
        }
    }

}
