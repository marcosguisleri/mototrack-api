package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripServiceTest {

    private TripService service;
    private Motorcycle honda;
    private Motorcycle yamaha;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        TripRepository repository = new InMemoryTripRepository();
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
        service = new TripService(repository, fixedClock);
        honda = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        yamaha = new Motorcycle(2, "Yamaha", "Tenere 700", 2024, 689);
    }

    @Test
    void shouldRegisterTrip() {
        Trip trip = createTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);

        service.registerTrip(trip);

        assertSame(trip, service.findTripById(1).orElseThrow());
    }

    @Test
    void shouldFindTripById() {
        Trip trip = createTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        service.registerTrip(trip);

        Trip result = service.findTripById(1).orElseThrow();

        assertSame(trip, result);
    }

    @Test
    void shouldChangeTripStatus() {
        Trip trip = createTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        service.registerTrip(trip);

        service.changeTripStatus(1, TripStatus.IN_PROGRESS);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        assertSame(trip, service.findTripById(1).orElseThrow());
    }

    @Test
    void shouldThrowWhenChangingStatusOfUnknownTrip() {
        assertThrows(
                TripNotFoundException.class,
                () -> service.changeTripStatus(99, TripStatus.IN_PROGRESS)
        );
    }

    @Test
    void shouldFindTripsByTerrain() {
        Trip asphaltTrip = createTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        Trip offRoadTrip = createTrip(2, TerrainType.OFF_ROAD, today.plusDays(2), yamaha);
        service.registerTrip(asphaltTrip);
        service.registerTrip(offRoadTrip);

        List<Trip> result = service.findTripsByTerrain(TerrainType.OFF_ROAD);

        assertEquals(1, result.size());
        assertSame(offRoadTrip, result.getFirst());
    }

    @Test
    void shouldFindPlannedTrips() {
        Trip plannedTrip = createTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        Trip inProgressTrip = createTrip(2, TerrainType.MIXED, today.plusDays(2), yamaha);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS, today);
        service.registerTrip(plannedTrip);
        service.registerTrip(inProgressTrip);

        List<Trip> result = service.findPlannedTrips();

        assertEquals(1, result.size());
        assertSame(plannedTrip, result.getFirst());
    }

    @Test
    void shouldFindTripsByDate() {
        LocalDate searchedDate = today.plusDays(5);
        Trip tripOnDate = createTrip(1, TerrainType.ASPHALT, searchedDate, honda);
        Trip tripOnOtherDate = createTrip(2, TerrainType.MIXED, searchedDate.plusDays(1), yamaha);
        service.registerTrip(tripOnDate);
        service.registerTrip(tripOnOtherDate);

        List<Trip> result = service.findTripsByDate(searchedDate);

        assertEquals(1, result.size());
        assertSame(tripOnDate, result.getFirst());
    }

    @Test
    void shouldFindUpcomingTrips() {
        Trip tripForToday = createTrip(1, TerrainType.ASPHALT, today, honda);
        Trip futureTrip = createTrip(2, TerrainType.MIXED, today.plusDays(2), yamaha);
        service.registerTrip(futureTrip);
        service.registerTrip(tripForToday);

        List<Trip> result = service.findUpcomingTrips();

        assertEquals(List.of(tripForToday, futureTrip), result);
        assertSame(tripForToday, result.getFirst());
    }

    @Test
    void shouldCalculateDaysUntilPlannedTrip() {
        Trip plannedTrip = createTrip(
                1,
                TerrainType.ASPHALT,
                today.plusDays(12),
                honda
        );
        service.registerTrip(plannedTrip);

        long result = service.calculateDaysUntilTrip(1);

        assertEquals(12, result);
    }

    @Test
    void shouldThrowWhenCalculatingDaysUntilCompletedTrip() {
        Trip completedTrip = Trip.registerCompleted(
                1,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                today.minusDays(1),
                honda,
                fixedClock
        );
        service.registerTrip(completedTrip);

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.calculateDaysUntilTrip(1)
        );
    }

    @Test
    void shouldRejectCompletingFutureTrip() {
        Trip trip = createTrip(
                1,
                TerrainType.ASPHALT,
                today.plusDays(10),
                honda
        );
        service.registerTrip(trip);

        service.changeTripStatus(1, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripDateException.class,
                () -> service.changeTripStatus(1, TripStatus.COMPLETED)
        );
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
    }

    private Trip createTrip(
            long id,
            TerrainType terrain,
            LocalDate plannedDate,
            Motorcycle motorcycle
    ) {
        return Trip.schedule(
                id,
                "Origem " + id,
                "Destino " + id,
                100 * id,
                terrain,
                plannedDate,
                motorcycle,
                fixedClock
        );
    }
}
