package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TripTest {

    private Motorcycle motorcycle;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        User owner = User.restore(1L, "Marcos", "marcos@example.com");
        motorcycle = Motorcycle.register(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                owner
        );
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
    }

    @Test
    void shouldCreateTripWithPlannedStatus() {
        Trip trip = createTrip(today.plusDays(1));

        assertEquals(TripStatus.PLANNED, trip.getStatus());
    }

    @Test
    void shouldCreateTripWithProvidedData() {
        LocalDate plannedDate = today.plusDays(10);

        Trip trip = Trip.schedule(
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                plannedDate,
                motorcycle,
                fixedClock
        );

        assertAll(
                () -> assertNull(trip.getId()),
                () -> assertEquals("Florianopolis", trip.getOrigin()),
                () -> assertEquals("Serra do Rio do Rastro", trip.getDestination()),
                () -> assertEquals(284.5, trip.getDistanceKm()),
                () -> assertEquals(TerrainType.ASPHALT, trip.getTerrain()),
                () -> assertEquals(plannedDate, trip.getTripDate()),
                () -> assertSame(motorcycle, trip.getMotorcycle())
        );
    }

    @Test
    void shouldAllowTripPlannedForToday() {
        Trip trip = assertDoesNotThrow(() -> createTrip(today));

        assertEquals(today, trip.getTripDate());
    }

    @Test
    void shouldRejectTripWithPastDate() {
        LocalDate yesterday = today.minusDays(1);

        assertThrows(
                InvalidTripDateException.class,
                () -> createTrip(yesterday)
        );
    }

    @Test
    void shouldRegisterCompletedTripForPastDate() {
        LocalDate yesterday = today.minusDays(1);

        Trip trip = assertDoesNotThrow(
                () -> createCompletedTrip(yesterday)
        );

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(yesterday, trip.getTripDate())
        );
    }

    @Test
    void shouldAllowCompletedTripForToday() {
        Trip trip = assertDoesNotThrow(
                () -> createCompletedTrip(today)
        );

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
    }

    @Test
    void shouldRejectCompletedTripWithFutureDate() {
        assertThrows(
                InvalidTripDateException.class,
                () -> createCompletedTrip(today.plusDays(1))
        );
    }

    @Test
    void shouldChangeStatusFromPlannedToInProgress() {
        Trip trip = createTrip(today.plusDays(1));

        trip.changeStatus(TripStatus.IN_PROGRESS, today);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
    }

    @Test
    void shouldChangeStatusFromInProgressToCompleted() {
        Trip trip = createTrip(today);
        trip.changeStatus(TripStatus.IN_PROGRESS, today);

        trip.changeStatus(TripStatus.COMPLETED, today);

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
    }

    @Test
    void shouldRejectTransitionFromPlannedToCompleted() {
        Trip trip = createTrip(today.plusDays(1));

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.COMPLETED, today)
        );
    }

    @Test
    void shouldRejectTransitionFromCompletedToInProgress() {
        Trip trip = createCompletedTrip();

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.IN_PROGRESS, today)
        );
    }

    @Test
    void shouldRejectTransitionFromCompletedToPlanned() {
        Trip trip = createCompletedTrip();

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.PLANNED, today)
        );
    }

    private Trip createTrip(LocalDate plannedDate) {
        return Trip.schedule(
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                plannedDate,
                motorcycle,
                fixedClock
        );
    }

    private Trip createCompletedTrip() {
        return createCompletedTrip(today.minusDays(1));
    }

    private Trip createCompletedTrip(LocalDate tripDate) {
        return Trip.registerCompleted(
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                tripDate,
                motorcycle,
                fixedClock
        );
    }
}
