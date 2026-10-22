package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripExpenseNotFoundException;
import br.dev.guisleri.mototrack.exception.InvalidTripExpenseException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripExpenseServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final long TRIP_ID = 5L;
    private static final long EXPENSE_ID = 9L;

    @Mock
    private TripExpenseRepository tripExpenseRepository;

    @Mock
    private TripService tripService;

    private TripExpenseService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        service = new TripExpenseService(tripExpenseRepository, tripService, fixedClock);
    }

    // -------------------------------------------------------------------------
    // Registro de abastecimento
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterTripExpenseOnTripInProgress() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.IN_PROGRESS));
        TripExpense persisted = expense(EXPENSE_ID);
        when(tripExpenseRepository.save(any(TripExpense.class))).thenReturn(persisted);
        ArgumentCaptor<TripExpense> captor = ArgumentCaptor.forClass(TripExpense.class);

        TripExpense result = service.registerExpenseForOwner(
                TRIP_ID, OWNER_ID, ExpenseCategory.TOLL, new BigDecimal("13.90"), null
        );

        verify(tripExpenseRepository).save(captor.capture());
        assertAll(
                () -> assertSame(persisted, result),
                () -> assertEquals(TRIP_ID, captor.getValue().getTripId()),
                () -> assertEquals(ExpenseCategory.TOLL, captor.getValue().getCategory()),
                () -> assertEquals(new BigDecimal("13.90"), captor.getValue().getAmount()),
                () -> assertEquals(fixedClock.instant(), captor.getValue().getCreatedAt())
        );
    }

    @Test
    void shouldRegisterTripExpenseOnCompletedTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(tripExpenseRepository.save(any(TripExpense.class)))
                .thenReturn(expense(EXPENSE_ID));

        service.registerExpenseForOwner(
                TRIP_ID, OWNER_ID, ExpenseCategory.FOOD, new BigDecimal("60"), null
        );

        verify(tripExpenseRepository).save(any(TripExpense.class));
    }

    @Test
    void shouldRejectTripExpenseOnPlannedTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.PLANNED));

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.registerExpenseForOwner(
                        TRIP_ID, OWNER_ID, ExpenseCategory.FOOD, new BigDecimal("60"), null
                )
        );
        verify(tripExpenseRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidTripExpenseValuesWithoutSaving() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.IN_PROGRESS));

        assertThrows(
                InvalidTripExpenseException.class,
                () -> service.registerExpenseForOwner(
                        TRIP_ID, OWNER_ID, ExpenseCategory.FOOD, BigDecimal.ZERO, null
                )
        );
        verify(tripExpenseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateAccessDeniedWhenRegisteringOnForeignTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenThrow(new TripAccessDeniedException("negado"));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.registerExpenseForOwner(
                        TRIP_ID, OWNER_ID, ExpenseCategory.FOOD, new BigDecimal("60"), null
                )
        );
        verify(tripExpenseRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Listagem
    // -------------------------------------------------------------------------

    @Test
    void shouldFindExpenseEntriesOfOwnedTrip() {
        List<TripExpense> entries = List.of(expense(1L), expense(2L));
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.PLANNED));
        when(tripExpenseRepository.findByTripId(TRIP_ID)).thenReturn(entries);

        assertSame(entries, service.findExpensesByTripIdForOwner(TRIP_ID, OWNER_ID));
    }

    @Test
    void shouldNotListExpenseEntriesOfForeignTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenThrow(new TripAccessDeniedException("negado"));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.findExpensesByTripIdForOwner(TRIP_ID, OWNER_ID)
        );
        verify(tripExpenseRepository, never()).findByTripId(TRIP_ID);
    }

    // -------------------------------------------------------------------------
    // Edição e exclusão
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateTripExpenseUnderLock() {
        TripExpense existing = expense(EXPENSE_ID);
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(tripExpenseRepository.findByIdAndTripIdForUpdate(EXPENSE_ID, TRIP_ID))
                .thenReturn(Optional.of(existing));
        when(tripExpenseRepository.save(existing)).thenReturn(existing);

        TripExpense result = service.updateExpenseForOwner(
                TRIP_ID, EXPENSE_ID, OWNER_ID, ExpenseCategory.PARKING, new BigDecimal("63.10"), "Centro"
        );

        assertAll(
                () -> assertSame(existing, result),
                () -> assertEquals(ExpenseCategory.PARKING, existing.getCategory()),
                () -> assertEquals(new BigDecimal("63.10"), existing.getAmount()),
                () -> assertEquals("Centro", existing.getDescription())
        );
        verify(tripExpenseRepository).save(existing);
    }

    @Test
    void shouldReportMissingTripExpenseWhenUpdating() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(tripExpenseRepository.findByIdAndTripIdForUpdate(EXPENSE_ID, TRIP_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TripExpenseNotFoundException.class,
                () -> service.updateExpenseForOwner(
                        TRIP_ID, EXPENSE_ID, OWNER_ID, ExpenseCategory.PARKING, new BigDecimal("60"), null
                )
        );
        verify(tripExpenseRepository, never()).save(any());
    }

    @Test
    void shouldDeleteTripExpenseUnderLock() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(tripExpenseRepository.findByIdAndTripIdForUpdate(EXPENSE_ID, TRIP_ID))
                .thenReturn(Optional.of(expense(EXPENSE_ID)));

        service.deleteExpenseForOwner(TRIP_ID, EXPENSE_ID, OWNER_ID);

        verify(tripExpenseRepository).deleteById(EXPENSE_ID);
    }

    @Test
    void shouldNotDeleteTripExpenseFromAnotherTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(tripExpenseRepository.findByIdAndTripIdForUpdate(EXPENSE_ID, TRIP_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TripExpenseNotFoundException.class,
                () -> service.deleteExpenseForOwner(TRIP_ID, EXPENSE_ID, OWNER_ID)
        );
        verify(tripExpenseRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Trip trip(TripStatus status) {
        User owner = User.restore(OWNER_ID, "Marcos", "marcos@example.com", "hash");
        Motorcycle motorcycle = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
        return Trip.restore(
                TRIP_ID,
                "Campinas",
                "Santos",
                170.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 14),
                motorcycle,
                status,
                null,
                null
        );
    }

    private TripExpense expense(long id) {
        return TripExpense.restore(
                id,
                TRIP_ID,
                ExpenseCategory.TOLL,
                new BigDecimal("13.90"),
                null,
                fixedClock.instant()
        );
    }
}
