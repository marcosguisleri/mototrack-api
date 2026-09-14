package br.dev.guisleri.mototrack.cli;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.InMemoryTripRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import br.dev.guisleri.mototrack.service.TripService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {

    void main() {
        TripRepository repository = new InMemoryTripRepository();
        TripService tripService = new TripService(repository);
        LocalDate today = LocalDate.now();

        Motorcycle hondaNx500 = new Motorcycle(
                1, "Honda", "NX 500", 2025, 471
        );
        Motorcycle yamahaTenere700 = new Motorcycle(
                2, "Yamaha", "Tenere 700", 2024, 689
        );
        Motorcycle royalEnfieldHimalayan = new Motorcycle(
                3, "Royal Enfield", "Himalayan 450", 2025, 452
        );

        // =========================
        // Cadastro das viagens
        // =========================

        List<Trip> tripsToRegister = List.of(
                new Trip(
                        1,
                        "Florianopolis",
                        "Serra do Rio do Rastro",
                        284.5,
                        TerrainType.ASPHALT,
                        today.plusDays(7),
                        hondaNx500
                ),
                new Trip(
                        2,
                        "Belo Horizonte",
                        "Ouro Preto",
                        310.0,
                        TerrainType.MIXED,
                        today.plusDays(14),
                        hondaNx500
                ),
                new Trip(
                        3,
                        "Sao Paulo",
                        "Serra Negra",
                        250.0,
                        TerrainType.ASPHALT,
                        today.plusDays(21),
                        hondaNx500
                ),
                new Trip(
                        4,
                        "Curitiba",
                        "Morretes",
                        225.5,
                        TerrainType.MIXED,
                        today.plusDays(30),
                        hondaNx500
                ),
                new Trip(
                        5,
                        "Palmas",
                        "Jalapao",
                        320.0,
                        TerrainType.OFF_ROAD,
                        today.plusDays(45),
                        hondaNx500
                ),
                new Trip(
                        6,
                        "Sao Paulo",
                        "Campos do Jordao",
                        252.5,
                        TerrainType.ASPHALT,
                        today.plusDays(60),
                        yamahaTenere700
                ),
                new Trip(
                        7,
                        "Goiania",
                        "Pirenopolis",
                        200.0,
                        TerrainType.MIXED,
                        today.plusDays(90),
                        royalEnfieldHimalayan
                )
        );

        tripsToRegister.forEach(tripService::registerTrip);

        IO.println("=== Viagens cadastradas ===");
        printTrips(tripService.findAllTrips());

        // =========================
        // Busca e filtros
        // =========================

        IO.println("\n=== Busca por id ===");

        tripService.findTripById(2)
                .ifPresentOrElse(
                        this::printTrip,
                        () -> IO.println("Viagem nao encontrada")
                );

        IO.println("\n=== Viagens por terreno: OFF_ROAD ===");
        printTrips(tripService.findTripsByTerrain(TerrainType.OFF_ROAD));

        IO.println("\n=== Viagens planejadas ===");
        printTrips(tripService.findPlannedTrips());

        IO.println("\n=== Viagens planejadas para daqui a 14 dias ===");
        printTrips(tripService.findTripsByDate(today.plusDays(14)));

        IO.println("\n=== Proximas viagens ===");
        printTrips(tripService.findUpcomingTrips());

        IO.println("\n=== Quantidade por status antes da conclusao ===");
        printCountByStatus(tripService.countTripsByStatus());

        // =========================
        // Alteracao de status
        // =========================

        tripService.findAllTrips()
                .forEach(trip -> completeTrip(tripService, trip.getId()));

        IO.println("\n=== Quantidade por status depois da conclusao ===");
        printCountByStatus(tripService.countTripsByStatus());

        // =========================
        // Cenarios de erro
        // =========================

        IO.println("\n=== Alteracao de status com id inexistente ===");

        try {
            tripService.changeTripStatus(99, TripStatus.IN_PROGRESS);
        } catch (TripNotFoundException exception) {
            IO.println(exception.getMessage());
        }

        IO.println("\n=== Cadastro com data no passado ===");

        try {
            tripService.registerTrip(
                    new Trip(
                            8,
                            "Sao Paulo",
                            "Viagem no tempo",
                            150.0,
                            TerrainType.ASPHALT,
                            today.minusDays(1),
                            hondaNx500
                    )
            );
        } catch (InvalidTripDateException exception) {
            IO.println(exception.getMessage());
        }

        IO.println("\n=== Transicao de status invalida ===");

        try {
            Trip invalidTransitionTrip = new Trip(
                    9,
                    "Araras",
                    "Brotas",
                    150.0,
                    TerrainType.ASPHALT,
                    today.plusDays(10),
                    hondaNx500
            );

            invalidTransitionTrip.changeStatus(TripStatus.COMPLETED);

        } catch (InvalidTripStatusException exception) {
            IO.println(exception.getMessage());
        }

        // =========================
        // Estatisticas e relatorio
        // =========================

        printReport(tripService);
    }

    private void completeTrip(TripService tripService, long tripId) {
        tripService.changeTripStatus(tripId, TripStatus.IN_PROGRESS);
        tripService.changeTripStatus(tripId, TripStatus.COMPLETED);
    }

    private void printTrips(List<Trip> trips) {
        trips.forEach(this::printTrip);
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

    private void printCountByStatus(Map<TripStatus, Long> countByStatus) {
        for (TripStatus status : TripStatus.values()) {
            System.out.printf(
                    "%s: %d%n",
                    status,
                    countByStatus.getOrDefault(status, 0L)
            );
        }
    }

    private void printReport(TripService tripService) {
        Map.Entry<Motorcycle, Long> mostUsedMotorcycle = tripService
                .countTripsByMotorcycle()
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        Motorcycle motorcycle = mostUsedMotorcycle.getKey();
        double motorcycleDistance = tripService
                .calculateCompletedDistanceByMotorcycle(motorcycle);

        Locale brazilianPortuguese = Locale.forLanguageTag("pt-BR");

        IO.println("\n=== Resumo MotoTrack ===");
        System.out.printf(
                brazilianPortuguese,
                """

                Viagens concluídas: %d
                Distância percorrida: %,.1f km

                Moto mais utilizada:
                %s %s
                %d viagens
                %,.0f km%n""",
                tripService.countCompletedTrips(),
                tripService.calculateTotalCompletedDistance(),
                motorcycle.getBrand(),
                motorcycle.getModel(),
                mostUsedMotorcycle.getValue(),
                motorcycleDistance
        );
    }
}
