package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripDateUpdateRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TripTest {

    private Motorcycle motorcycle;
    private Clock fixedClock;
    private LocalDate today;
    private Instant now;

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
        now = fixedClock.instant();
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

        trip.startTrip(today, false, now, now);

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
                () -> trip.startTrip(today, false, now, now)
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldStartEarlyTripAndUpdateDateWhenConfirmed() {
        Trip trip = createTrip(today.plusDays(1));

        trip.startTrip(today, true, now, now);

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
                () -> trip.startTrip(today, false, now, now)
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldStartLateTripAndUpdateDateWhenConfirmed() {
        Trip trip = restoreTrip(today.minusDays(1), TripStatus.PLANNED);

        trip.startTrip(today, true, now, now);

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
                () -> trip.startTrip(today, false, now, now)
        );
    }

    @Test
    void shouldRejectStartingTripWhenStatusIsCompleted() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.startTrip(today, false, now, now)
        );
    }

    // -------------------------------------------------------------------------
    // Horários de início e conclusão
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateTripsWithoutTimestamps() {
        Trip planned = createTrip(today.plusDays(1));
        Trip completed = createCompletedTrip(today);

        assertAll(
                () -> assertNull(planned.getStartedAt()),
                () -> assertNull(planned.getCompletedAt()),
                () -> assertNull(completed.getStartedAt()),
                () -> assertNull(completed.getCompletedAt())
        );
    }

    @Test
    void shouldRecordStartedAtWhenStartingTrip() {
        Trip trip = createTrip(today);
        Instant startedAt = now.minusSeconds(3600);

        trip.startTrip(today, false, startedAt, now);

        assertEquals(startedAt, trip.getStartedAt());
    }

    @Test
    void shouldRejectStartedAtInTheFuture() {
        Trip trip = createTrip(today);

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.startTrip(today, false, now.plusSeconds(1), now)
        );
        assertNull(trip.getStartedAt());
        assertEquals(TripStatus.PLANNED, trip.getStatus());
    }

    @Test
    void shouldRecordCompletedAtWhenCompletingTrip() {
        Trip trip = createTrip(today);
        Instant startedAt = now.minusSeconds(7200);
        Instant completedAt = now.minusSeconds(600);
        trip.startTrip(today, false, startedAt, now);

        trip.completeTrip(today, false, completedAt, now);

        assertAll(
                () -> assertEquals(startedAt, trip.getStartedAt()),
                () -> assertEquals(completedAt, trip.getCompletedAt())
        );
    }

    @Test
    void shouldRejectCompletedAtBeforeStartedAt() {
        Trip trip = createTrip(today);
        trip.startTrip(today, false, now.minusSeconds(600), now);

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.completeTrip(today, false, now.minusSeconds(601), now)
        );
        assertNull(trip.getCompletedAt());
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
    }

    @Test
    void shouldRejectCompletedAtInTheFuture() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.completeTrip(today, false, now.plusSeconds(1), now)
        );
        assertNull(trip.getCompletedAt());
    }

    @Test
    void shouldCompleteTripInProgressWithoutStartedAt() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, false, now, now);

        assertAll(
                () -> assertNull(trip.getStartedAt()),
                () -> assertEquals(now, trip.getCompletedAt()),
                () -> assertTrue(trip.duration().isEmpty())
        );
    }

    @Test
    void shouldCalculateDurationFromTimestamps() {
        Trip trip = createTrip(today);
        trip.startTrip(today, false, now.minusSeconds(7200), now);
        trip.completeTrip(today, false, now, now);

        assertEquals(Duration.ofHours(2), trip.duration().orElseThrow());
    }

    @Test
    void shouldHaveNoDurationBeforeCompletion() {
        Trip trip = createTrip(today);
        trip.startTrip(today, false, now, now);

        assertTrue(trip.duration().isEmpty());
    }

    @Test
    void shouldRestoreTimestamps() {
        Trip trip = Trip.restore(
                1L,
                "Florianopolis",
                "Serra do Rio do Rastro",
                284.5,
                TerrainType.ASPHALT,
                today,
                motorcycle,
                TripStatus.COMPLETED,
                now.minusSeconds(60),
                now
        );

        assertAll(
                () -> assertEquals(now.minusSeconds(60), trip.getStartedAt()),
                () -> assertEquals(now, trip.getCompletedAt())
        );
    }

    // -------------------------------------------------------------------------
    // Edição dos dados da viagem
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdatePlannedTripDetailsAndKeepLifecycle() {
        Trip trip = createTrip(today.plusDays(1));
        Motorcycle otherMotorcycle = Motorcycle.restore(
                2L, "Yamaha", "Tenere 700", "Blue", 2024, 689, MotorcycleType.NAKED,
                motorcycle.getOwner()
        );

        trip.updateDetails(
                "Campinas", "Santos", 170.0, TerrainType.MIXED,
                today.plusDays(5), otherMotorcycle, today
        );

        assertAll(
                () -> assertEquals("Campinas", trip.getOrigin()),
                () -> assertEquals("Santos", trip.getDestination()),
                () -> assertEquals(170.0, trip.getDistanceKm()),
                () -> assertEquals(TerrainType.MIXED, trip.getTerrain()),
                () -> assertEquals(today.plusDays(5), trip.getTripDate()),
                () -> assertSame(otherMotorcycle, trip.getMotorcycle()),
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus())
        );
    }

    @Test
    void shouldAllowPlannedTripOnToday() {
        Trip trip = createTrip(today.plusDays(1));

        trip.updateDetails(
                "A", "B", 10.0, TerrainType.ASPHALT, today, motorcycle, today
        );

        assertEquals(today, trip.getTripDate());
    }

    @Test
    void shouldRejectPlannedTripInThePastKeepingPreviousValues() {
        Trip trip = createTrip(today.plusDays(1));

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.updateDetails(
                        "A", "B", 10.0, TerrainType.ASPHALT,
                        today.minusDays(1), motorcycle, today
                )
        );
        assertAll(
                () -> assertEquals("Florianopolis", trip.getOrigin()),
                () -> assertEquals(today.plusDays(1), trip.getTripDate())
        );
    }

    @Test
    void shouldUpdateTripInProgressAndCompletedWithPastOrCurrentDate() {
        Trip inProgress = restoreTrip(today, TripStatus.IN_PROGRESS);
        Trip completed = restoreTrip(today.minusDays(3), TripStatus.COMPLETED);

        inProgress.updateDetails(
                "A", "B", 10.0, TerrainType.OFF_ROAD, today, motorcycle, today
        );
        completed.updateDetails(
                "C", "D", 20.0, TerrainType.MIXED, today.minusDays(10), motorcycle, today
        );

        assertAll(
                () -> assertEquals("B", inProgress.getDestination()),
                () -> assertEquals(TripStatus.IN_PROGRESS, inProgress.getStatus()),
                () -> assertEquals(today.minusDays(10), completed.getTripDate()),
                () -> assertEquals(TripStatus.COMPLETED, completed.getStatus())
        );
    }

    @Test
    void shouldRejectFutureDateForTripInProgressAndCompleted() {
        Trip inProgress = restoreTrip(today, TripStatus.IN_PROGRESS);
        Trip completed = restoreTrip(today, TripStatus.COMPLETED);

        assertAll(
                () -> assertThrows(
                        InvalidTripDateException.class,
                        () -> inProgress.updateDetails(
                                "A", "B", 10.0, TerrainType.ASPHALT,
                                today.plusDays(1), motorcycle, today
                        )
                ),
                () -> assertThrows(
                        InvalidTripDateException.class,
                        () -> completed.updateDetails(
                                "A", "B", 10.0, TerrainType.ASPHALT,
                                today.plusDays(1), motorcycle, today
                        )
                )
        );
    }

    @Test
    void shouldKeepTimestampsWhenUpdatingDetails() {
        Trip trip = createTrip(today);
        trip.startTrip(today, false, now.minusSeconds(60), now);
        trip.completeTrip(today, false, now, now);

        trip.updateDetails(
                "A", "B", 10.0, TerrainType.ASPHALT, today, motorcycle, today
        );

        assertAll(
                () -> assertEquals(now.minusSeconds(60), trip.getStartedAt()),
                () -> assertEquals(now, trip.getCompletedAt())
        );
    }

    // -------------------------------------------------------------------------
    // Correção dos horários
    // -------------------------------------------------------------------------

    @Test
    void shouldCorrectBothTimestampsOfCompletedTrip() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        trip.correctTimestamps(now.minusSeconds(7200), now.minusSeconds(600), now);

        assertAll(
                () -> assertEquals(now.minusSeconds(7200), trip.getStartedAt()),
                () -> assertEquals(now.minusSeconds(600), trip.getCompletedAt()),
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus())
        );
    }

    @Test
    void shouldClearTimestampsOfCompletedTripWhenNull() {
        Trip trip = Trip.restore(
                1L, "A", "B", 10.0, TerrainType.ASPHALT, today, motorcycle,
                TripStatus.COMPLETED, now.minusSeconds(60), now
        );

        trip.correctTimestamps(null, null, now);

        assertAll(
                () -> assertNull(trip.getStartedAt()),
                () -> assertNull(trip.getCompletedAt()),
                () -> assertTrue(trip.duration().isEmpty())
        );
    }

    @Test
    void shouldAcceptCompletedAtWithoutStartedAt() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        trip.correctTimestamps(null, now, now);

        assertAll(
                () -> assertNull(trip.getStartedAt()),
                () -> assertEquals(now, trip.getCompletedAt())
        );
    }

    @Test
    void shouldCorrectOnlyStartedAtOfTripInProgress() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        trip.correctTimestamps(now.minusSeconds(300), null, now);

        assertAll(
                () -> assertEquals(now.minusSeconds(300), trip.getStartedAt()),
                () -> assertNull(trip.getCompletedAt())
        );
    }

    @Test
    void shouldRejectCompletedAtForTripInProgress() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.correctTimestamps(now.minusSeconds(300), now, now)
        );
        assertNull(trip.getStartedAt());
    }

    @Test
    void shouldRejectTimestampsForPlannedTrip() {
        Trip trip = createTrip(today.plusDays(1));

        assertThrows(
                InvalidTripStatusException.class,
                () -> trip.correctTimestamps(now.minusSeconds(60), null, now)
        );
    }

    @Test
    void shouldRejectFutureTimestampsOnCorrection() {
        Trip trip = restoreTrip(today, TripStatus.COMPLETED);

        assertAll(
                () -> assertThrows(
                        InvalidTripDateException.class,
                        () -> trip.correctTimestamps(now.plusSeconds(1), null, now)
                ),
                () -> assertThrows(
                        InvalidTripDateException.class,
                        () -> trip.correctTimestamps(null, now.plusSeconds(1), now)
                )
        );
    }

    @Test
    void shouldRejectCompletedAtBeforeStartedAtOnCorrectionKeepingPreviousValues() {
        Trip trip = Trip.restore(
                1L, "A", "B", 10.0, TerrainType.ASPHALT, today, motorcycle,
                TripStatus.COMPLETED, now.minusSeconds(60), now
        );

        assertThrows(
                InvalidTripDateException.class,
                () -> trip.correctTimestamps(now.minusSeconds(10), now.minusSeconds(20), now)
        );
        assertAll(
                () -> assertEquals(now.minusSeconds(60), trip.getStartedAt()),
                () -> assertEquals(now, trip.getCompletedAt())
        );
    }

    // -------------------------------------------------------------------------
    // Registros reais (abastecimentos e gastos)
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectRecordsOnPlannedTrip() {
        Trip trip = createTrip(today.plusDays(1));

        assertThrows(InvalidTripStatusException.class, trip::assertAcceptsRecords);
    }

    @Test
    void shouldAcceptRecordsOnTripInProgressAndCompleted() {
        assertAll(
                () -> assertDoesNotThrow(
                        restoreTrip(today, TripStatus.IN_PROGRESS)::assertAcceptsRecords
                ),
                () -> assertDoesNotThrow(
                        restoreTrip(today, TripStatus.COMPLETED)::assertAcceptsRecords
                )
        );
    }

    // -------------------------------------------------------------------------
    // Conclusão de viagem e validação das transições
    // -------------------------------------------------------------------------

    @Test
    void shouldCompleteTripInProgressOnCurrentDate() {
        Trip trip = restoreTrip(today, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, false, now, now);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldCompletePastTripInProgress() {
        LocalDate originalDate = today.minusDays(1);
        Trip trip = restoreTrip(originalDate, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, false, now, now);

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
                () -> trip.completeTrip(today, false, now, now)
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
                () -> trip.completeTrip(today, false, now, now)
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
                () -> trip.completeTrip(today, false, now, now)
        );

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldCompleteTripInProgressWithFutureDateWhenUpdateIsConfirmed() {
        Trip trip = restoreTrip(today.plusDays(4), TripStatus.IN_PROGRESS);

        trip.completeTrip(today, true, now, now);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
    }

    @Test
    void shouldKeepPastDateWhenCompletingEvenIfUpdateIsRequested() {
        LocalDate originalDate = today.minusDays(2);
        Trip trip = restoreTrip(originalDate, TripStatus.IN_PROGRESS);

        trip.completeTrip(today, true, now, now);

        assertAll(
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
    }

    @Test
    void shouldCompleteTripAfterStartingWithUpdatedDate() {
        Trip trip = createTrip(today.plusDays(1));

        trip.startTrip(today, true, now, now);
        trip.completeTrip(today, false, now, now);

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
                status,
                null,
                null
        );
    }
}
