package br.dev.guisleri.mototrack.cli;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.repository.InMemoryTripRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import br.dev.guisleri.mototrack.service.TripService;

import java.time.LocalDate;

public class Main {

    void main() {
        TripRepository repository = new InMemoryTripRepository();
        TripService tripService = new TripService(repository);

        Motorcycle motorcycle = new Motorcycle(
                1,
                "Honda",
                "NX 500",
                2025,
                471
        );

        Trip trip = new Trip(
                1,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                LocalDate.now().plusDays(7),
                motorcycle
        );

        tripService.registerTrip(trip);

        IO.println("=== Viagens cadastradas ===");
        tripService.findAllTrips().forEach(this::printTrip);
    }

    private void printTrip(Trip trip) {
        Motorcycle motorcycle = trip.getMotorcycle();

        System.out.printf(
                "Id: %d | Rota: %s -> %s | Distancia: %.1f km | "
                        + "Terreno: %s | Status: %s | Data: %s | Moto: %s %s%n",
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getStatus(),
                trip.getPlannedDate(),
                motorcycle.getBrand(),
                motorcycle.getModel()
        );
    }
}
