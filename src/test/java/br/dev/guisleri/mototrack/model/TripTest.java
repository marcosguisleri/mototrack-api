package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TripTest {

    private Motorcycle motorcycle;

    @BeforeEach
    void setUp() {
        motorcycle = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
    }

    @Test
    void shouldCreateTripWithPlannedStatus() {
        Trip trip = createTrip(LocalDate.now().plusDays(1));

        assertEquals(TripStatus.PLANNED, trip.getStatus());
    }

    @Test
    void shouldCreateTripWithProvidedData() {
        LocalDate plannedDate = LocalDate.now().plusDays(10);

        Trip trip = new Trip(
                10,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                plannedDate,
                motorcycle
        );

        assertAll(
                () -> assertEquals(10, trip.getId()),
                () -> assertEquals("Florianopolis", trip.getOrigin()),
                () -> assertEquals("Serra do Rio do Rastro", trip.getDestination()),
                () -> assertEquals(284.5, trip.getDistanceKm()),
                () -> assertEquals(TerrainType.ASPHALT, trip.getTerrain()),
                () -> assertEquals(plannedDate, trip.getPlannedDate()),
                () -> assertSame(motorcycle, trip.getMotorcycle())
        );
    }

    @Test
    void shouldAllowTripPlannedForToday() {
        LocalDate today = LocalDate.now();

        Trip trip = assertDoesNotThrow(() -> createTrip(today));

        assertEquals(today, trip.getPlannedDate());
    }

    @Test
    void shouldRejectTripWithPastDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThrows(
                InvalidTripDateException.class,
                () -> createTrip(yesterday)
        );
    }

    @Test
    void shouldCalculateDaysUntilPlannedDate() {
        Trip trip = createTrip(LocalDate.now().plusDays(12));

        assertEquals(12, trip.getDaysUntilPlannedDate());
    }

    @Test
    void shouldChangeStatusFromPlannedToInProgress() {
        Trip trip = createTrip(LocalDate.now().plusDays(1));

        trip.changeStatus(TripStatus.IN_PROGRESS);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
    }

    @Test
    void shouldChangeStatusFromInProgressToCompleted() {
        Trip trip = createTrip(LocalDate.now().plusDays(1));
        trip.changeStatus(TripStatus.IN_PROGRESS);

        trip.changeStatus(TripStatus.COMPLETED);

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
    }

    @Test
    void shouldRejectTransitionFromPlannedToCompleted() {
        Trip trip = createTrip(LocalDate.now().plusDays(1));

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.COMPLETED)
        );
    }

    @Test
    void shouldRejectTransitionFromCompletedToInProgress() {
        Trip trip = createCompletedTrip();

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.IN_PROGRESS)
        );
    }

    @Test
    void shouldRejectTransitionFromCompletedToPlanned() {
        Trip trip = createCompletedTrip();

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.changeStatus(TripStatus.PLANNED)
        );
    }

    private Trip createTrip(LocalDate plannedDate) {
        return new Trip(
                1,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                plannedDate,
                motorcycle
        );
    }

    private Trip createCompletedTrip() {
        Trip trip = createTrip(LocalDate.now().plusDays(1));
        trip.changeStatus(TripStatus.IN_PROGRESS);
        trip.changeStatus(TripStatus.COMPLETED);
        return trip;
    }
}
