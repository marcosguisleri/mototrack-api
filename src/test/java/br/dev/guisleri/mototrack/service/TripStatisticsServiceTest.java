package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.InMemoryTripRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripStatisticsServiceTest {

    private TripRepository repository;
    private TripStatisticsService statisticsService;
    private Motorcycle honda;
    private Motorcycle yamaha;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTripRepository();
        statisticsService = new TripStatisticsService(repository);
        honda = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        yamaha = new Motorcycle(2, "Yamaha", "Tenere 700", 2024, 689);
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
    }

    @Test
    void shouldCountTripsByStatus() {
        Trip plannedTrip = createTrip(1, 100, honda);
        Trip inProgressTrip = createTrip(2, 200, yamaha);
        Trip completedTrip = createCompletedTrip(3, 300, honda, -3);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS, today);
        repository.save(plannedTrip);
        repository.save(inProgressTrip);
        repository.save(completedTrip);

        Map<TripStatus, Long> result = statisticsService.countTripsByStatus();

        assertEquals(1L, result.get(TripStatus.PLANNED));
        assertEquals(1L, result.get(TripStatus.IN_PROGRESS));
        assertEquals(1L, result.get(TripStatus.COMPLETED));
    }

    @Test
    void shouldCountCompletedTrips() {
        Trip completedTrip = createCompletedTrip(1, 100, honda, -1);
        Trip plannedTrip = createTrip(2, 200, yamaha);
        repository.save(completedTrip);
        repository.save(plannedTrip);

        assertEquals(1, statisticsService.countCompletedTrips());
    }

    @Test
    void shouldCalculateTotalCompletedDistance() {
        Trip firstCompletedTrip = createCompletedTrip(1, 100.5, honda, -1);
        Trip secondCompletedTrip = createCompletedTrip(2, 149.5, yamaha, -2);
        Trip plannedTrip = createTrip(3, 500, honda);
        repository.save(firstCompletedTrip);
        repository.save(secondCompletedTrip);
        repository.save(plannedTrip);

        double result = statisticsService.calculateTotalCompletedDistance();

        assertEquals(250, result, 0.001);
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        repository.save(createTrip(1, 100, honda));
        repository.save(createTrip(2, 200, honda));
        repository.save(createTrip(3, 300, yamaha));

        Map<Motorcycle, Long> result = statisticsService.countTripsByMotorcycle();

        assertEquals(2L, result.get(honda));
        assertEquals(1L, result.get(yamaha));
    }

    @Test
    void shouldCalculateCompletedDistanceByMotorcycle() {
        Trip completedHondaTrip = createCompletedTrip(1, 100, honda, -1);
        Trip plannedHondaTrip = createTrip(2, 200, honda);
        Trip completedYamahaTrip = createCompletedTrip(3, 300, yamaha, -3);
        repository.save(completedHondaTrip);
        repository.save(plannedHondaTrip);
        repository.save(completedYamahaTrip);

        double result = statisticsService
                .calculateCompletedDistanceByMotorcycle(honda);

        assertEquals(100, result, 0.001);
    }

    @Test
    void shouldFindMostUsedMotorcycle() {
        Trip firstCompletedHondaTrip = createCompletedTrip(1, 100, honda, -1);
        Trip secondCompletedHondaTrip = createCompletedTrip(2, 200, honda, -2);
        Trip completedYamahaTrip = createCompletedTrip(3, 300, yamaha, -3);
        Trip plannedYamahaTrip = createTrip(4, 400, yamaha);
        repository.save(firstCompletedHondaTrip);
        repository.save(secondCompletedHondaTrip);
        repository.save(completedYamahaTrip);
        repository.save(plannedYamahaTrip);

        Optional<Motorcycle> result = statisticsService.getMostUsedMotorcycle();

        assertEquals(Optional.of(honda), result);
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoCompletedTrips() {
        Trip plannedTrip = createTrip(1, 100, honda);
        Trip inProgressTrip = createTrip(2, 200, yamaha);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS, today);
        repository.save(plannedTrip);
        repository.save(inProgressTrip);

        Optional<Motorcycle> result = statisticsService.getMostUsedMotorcycle();

        assertTrue(result.isEmpty());
    }

    private Trip createTrip(long id, double distance, Motorcycle motorcycle) {
        return Trip.schedule(
                id,
                "Origem " + id,
                "Destino " + id,
                distance,
                TerrainType.ASPHALT,
                today.plusDays(id),
                motorcycle,
                fixedClock
        );
    }

    private Trip createCompletedTrip(
            long id,
            double distance,
            Motorcycle motorcycle,
            int daysFromToday
    ) {
        return Trip.registerCompleted(
                id,
                "Origem " + id,
                "Destino " + id,
                distance,
                TerrainType.ASPHALT,
                today.plusDays(daysFromToday),
                motorcycle,
                fixedClock
        );
    }
}
