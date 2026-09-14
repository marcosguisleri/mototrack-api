package br.dev.guisleri.mototrack.cli;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.repository.InMemoryTripRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import br.dev.guisleri.mototrack.service.TripService;

import java.time.Clock;
import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {
        TripRepository repository = new InMemoryTripRepository();
        Clock clock = Clock.systemDefaultZone();
        TripService tripService = new TripService(repository, clock);

        Motorcycle motorcycle = new Motorcycle(
                1,
                "Honda",
                "NX 500",
                2025,
                471
        );

        Trip trip = Trip.schedule(
                1,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                LocalDate.now(clock).plusDays(7),
                motorcycle,
                clock
        );

        tripService.registerTrip(trip);

        IO.println("=== Viagens cadastradas ===");
        tripService.findAllTrips().forEach(Main::printTrip);
    }

    private static void printTrip(Trip trip) {
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
                trip.getTripDate(),
                motorcycle.getBrand(),
                motorcycle.getModel()
        );
    }
}
