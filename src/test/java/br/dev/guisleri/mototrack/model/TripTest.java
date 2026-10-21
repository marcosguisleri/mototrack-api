package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripDateUpdateRequiredException;
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
        User owner = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        motorcycle = Motorcycle.register(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                MotorcycleType.NAKED,
                owner
        );
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
    }

    // -------------------------------------------------------------------------
    // Agendamento de viagens planejadas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Registro de viagens já concluídas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Início de viagem e confirmação da atualização de data
    // -------------------------------------------------------------------------

    @Test
    void shouldStartTripOnScheduledDate() {
        Trip trip = createTrip(today);

        trip.startTrip(today, false);

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldRejectEarlyStartWithoutDateUpdateConfirmation() {
        LocalDate originalDate = today.plusDays(1);
        Trip trip = createTrip(originalDate);

        assertThrows(
                TripDateUpdateRequiredException.class,
                () -> trip.startTrip(today, false)
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldStartEarlyTripAndUpdateDateWhenConfirmed() {
        Trip trip = createTrip(today.plusDays(1));

        trip.startTrip(today, true);

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldRejectLateStartWithoutDateUpdateConfirmation() {
        LocalDate originalDate = today.minusDays(1);
        Trip trip = restoreTrip(originalDate, TripStatus.PLANNED);

        assertThrows(
                TripDateUpdateRequiredException.class,
                () -> trip.startTrip(today, false)
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldStartLateTripAndUpdateDateWhenConfirmed() {
        Trip trip = restoreTrip(today.minusDays(1), TripStatus.PLANNED);

        trip.startTrip(today, true);

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldRejectStartingTripWhenStatusIsInProgress() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.startTrip(today, false)
        );
    }

    @Test
    void shouldRejectStartingTripWhenStatusIsCompleted() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.startTrip(today, false)
        );
    }

    // -------------------------------------------------------------------------
    // Conclusão de viagem e validação das transições
    // -------------------------------------------------------------------------

    @Test
    void shouldCompleteTripInProgressOnCurrentDate() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, false);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldCompletePastTripInProgress() {
        LocalDate originalDate = today.minusDays(1);
        Trip trip = restoreTrip(originalDate, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, false);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldRejectCompletingPlannedTrip() {
        Trip trip = createTrip(today);

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.completeTrip(today, false)
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldRejectCompletingCompletedTrip() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.completeTrip(today, false)
        );

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldRequireConfirmationToCompleteTripInProgressWithFutureDate() {
        LocalDate originalDate = today.plusDays(1);
        Trip trip = restoreTrip(originalDate, TripStatus.IN_PROGRESS);

        assertThrows(
                TripDateUpdateRequiredException.class,
                () -> trip.completeTrip(today, false)
        );

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldCompleteTripInProgressWithFutureDateWhenUpdateIsConfirmed() {
        Trip trip = restoreTrip(today.plusDays(4), TripStatus.IN_PROGRESS);

        trip.completeTrip(today, true);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldKeepPastDateWhenCompletingEvenIfUpdateIsRequested() {
        LocalDate originalDate = today.minusDays(2);
        Trip trip = restoreTrip(originalDate, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, true);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldCompleteTripAfterStartingWithUpdatedDate() {
        Trip trip = createTrip(today.plusDays(1));

        trip.startTrip(today, true);
        trip.completeTrip(today, false);

        assertAll(
                () -> assertEquals(today, trip.getTripDate()),
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus())
        );
    }

    // -------------------------------------------------------------------------
    // Helpers de criação e restauração do domínio
    // -------------------------------------------------------------------------

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

    private Trip restoreTrip(LocalDate tripDate, TripStatus status) {
        return Trip.restore(
                1L,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                tripDate,
                motorcycle,
                status
        );
    }
}
