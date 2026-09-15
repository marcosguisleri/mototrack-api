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
    void shouldScheduleTrip() {
        Trip trip = scheduleTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);

        assertEquals(trip.getId(), service.findTripById(1).getId());
    }

    @Test
    void shouldFindTripById() {
        Trip trip = scheduleTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);

        Trip result = service.findTripById(1);

        assertEquals(trip.getId(), result.getId());
    }

    @Test
    void shouldThrowWhenFindingUnknownTrip() {
        assertThrows(
                TripNotFoundException.class,
                () -> service.findTripById(99)
        );
    }

    @Test
    void shouldChangeTripStatus() {
        Trip trip = scheduleTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);

        service.changeTripStatus(1, TripStatus.IN_PROGRESS);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        assertEquals(trip.getId(), service.findTripById(1).getId());
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
        Trip asphaltTrip = scheduleTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        Trip offRoadTrip = scheduleTrip(2, TerrainType.OFF_ROAD, today.plusDays(2), yamaha);

        List<Trip> result = service.findTripsByTerrain(TerrainType.OFF_ROAD);

        assertEquals(1, result.size());
        assertEquals(offRoadTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByStatus() {
        Trip plannedTrip = scheduleTrip(1, TerrainType.ASPHALT, today.plusDays(1), honda);
        scheduleTrip(2, TerrainType.MIXED, today.plusDays(2), yamaha);
        service.changeTripStatus(2, TripStatus.IN_PROGRESS);

        List<Trip> result = service.findTripsByStatus(TripStatus.PLANNED);

        assertEquals(1, result.size());
        assertEquals(plannedTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByDate() {
        LocalDate searchedDate = today.plusDays(5);
        Trip tripOnDate = scheduleTrip(1, TerrainType.ASPHALT, searchedDate, honda);
        scheduleTrip(2, TerrainType.MIXED, searchedDate.plusDays(1), yamaha);

        List<Trip> result = service.findTripsByDate(searchedDate);

        assertEquals(1, result.size());
        assertEquals(tripOnDate.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindUpcomingTrips() {
        Trip futureTrip = scheduleTrip(2, TerrainType.MIXED, today.plusDays(2), yamaha);
        Trip tripForToday = scheduleTrip(1, TerrainType.ASPHALT, today, honda);

        List<Trip> result = service.findUpcomingTrips();

        assertEquals(List.of(tripForToday, futureTrip), result);
        assertEquals(tripForToday.getId(), result.getFirst().getId());
    }

    @Test
    void shouldCalculateDaysUntilPlannedTrip() {
        scheduleTrip(
                1,
                TerrainType.ASPHALT,
                today.plusDays(12),
                honda
        );

        long result = service.calculateDaysUntilTrip(1);

        assertEquals(12, result);
    }

    @Test
    void shouldThrowWhenCalculatingDaysUntilCompletedTrip() {
        service.registerCompletedTrip(
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                today.minusDays(1),
                honda
        );

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.calculateDaysUntilTrip(1)
        );
    }

    @Test
    void shouldRejectCompletingFutureTrip() {
        Trip trip = scheduleTrip(
                1,
                TerrainType.ASPHALT,
                today.plusDays(10),
                honda
        );

        service.changeTripStatus(1, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripDateException.class,
                () -> service.changeTripStatus(1, TripStatus.COMPLETED)
        );
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
    }

    private Trip scheduleTrip(
            long id,
            TerrainType terrain,
            LocalDate plannedDate,
            Motorcycle motorcycle
    ) {
        return service.scheduleTrip(
                "Origem " + id,
                "Destino " + id,
                100 * id,
                terrain,
                plannedDate,
                motorcycle
        );
    }
}
