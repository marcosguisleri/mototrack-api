package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripExpenseException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripExpenseTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Test
    void shouldRegisterExpenseWithCreationInstant() {
        TripExpense expense = TripExpense.register(
                7L, ExpenseCategory.FOOD, new BigDecimal("42.50"), "Almoço", fixedClock
        );

        assertAll(
                () -> assertNull(expense.getId()),
                () -> assertEquals(7L, expense.getTripId()),
                () -> assertEquals(ExpenseCategory.FOOD, expense.getCategory()),
                () -> assertEquals(new BigDecimal("42.50"), expense.getAmount()),
                () -> assertEquals("Almoço", expense.getDescription()),
                () -> assertEquals(fixedClock.instant(), expense.getCreatedAt())
        );
    }

    @Test
    void shouldNormalizeBlankDescriptionToNullAndTrimText() {
        TripExpense blank = TripExpense.register(
                7L, ExpenseCategory.TOLL, new BigDecimal("13.90"), "   ", fixedClock
        );
        TripExpense padded = TripExpense.register(
                7L, ExpenseCategory.TOLL, new BigDecimal("13.90"), "  Praça 2 ", fixedClock
        );
        TripExpense missing = TripExpense.register(
                7L, ExpenseCategory.TOLL, new BigDecimal("13.90"), null, fixedClock
        );

        assertAll(
                () -> assertNull(blank.getDescription()),
                () -> assertEquals("Praça 2", padded.getDescription()),
                () -> assertNull(missing.getDescription())
        );
    }

    @Test
    void shouldRejectMissingCategory() {
        assertThrows(
                InvalidTripExpenseException.class,
                () -> TripExpense.register(
                        7L, null, new BigDecimal("10"), null, fixedClock
                )
        );
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        assertAll(
                () -> assertThrows(
                        InvalidTripExpenseException.class,
                        () -> TripExpense.register(
                                7L, ExpenseCategory.OTHER, BigDecimal.ZERO, null, fixedClock
                        )
                ),
                () -> assertThrows(
                        InvalidTripExpenseException.class,
                        () -> TripExpense.register(
                                7L, ExpenseCategory.OTHER, new BigDecimal("-1"), null, fixedClock
                        )
                )
        );
    }

    @Test
    void shouldUpdateKeepingCreationInstant() {
        TripExpense expense = TripExpense.restore(
                1L, 7L, ExpenseCategory.OTHER, new BigDecimal("631.00"), null,
                fixedClock.instant()
        );

        expense.update(ExpenseCategory.PARKING, new BigDecimal("63.10"), "Estacionamento");

        assertAll(
                () -> assertEquals(ExpenseCategory.PARKING, expense.getCategory()),
                () -> assertEquals(new BigDecimal("63.10"), expense.getAmount()),
                () -> assertEquals("Estacionamento", expense.getDescription()),
                () -> assertEquals(fixedClock.instant(), expense.getCreatedAt())
        );
    }

    @Test
    void shouldRejectInvalidUpdateAndKeepPreviousValues() {
        TripExpense expense = TripExpense.restore(
                1L, 7L, ExpenseCategory.TOLL, new BigDecimal("13.90"), "Praça",
                fixedClock.instant()
        );

        assertThrows(
                InvalidTripExpenseException.class,
                () -> expense.update(ExpenseCategory.FOOD, BigDecimal.ZERO, "x")
        );
        assertAll(
                () -> assertEquals(ExpenseCategory.TOLL, expense.getCategory()),
                () -> assertEquals(new BigDecimal("13.90"), expense.getAmount()),
                () -> assertEquals("Praça", expense.getDescription())
        );
    }
}
