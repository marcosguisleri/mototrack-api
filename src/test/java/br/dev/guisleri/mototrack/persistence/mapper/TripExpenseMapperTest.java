package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.persistence.entity.TripExpenseEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TripExpenseMapperTest {

    private final TripExpenseMapper tripExpenseMapper = new TripExpenseMapper();

    @Test
    void shouldMapExpenseToEntity() {
        Instant createdAt = Instant.parse("2026-09-14T12:00:00Z");
        TripExpense expense = TripExpense.restore(
                3L, 42L, ExpenseCategory.FOOD, new BigDecimal("42.50"), "Almoço", createdAt
        );
        TripEntity tripEntity = new TripEntity(
                42L, "A", "B", 10.0, null, null, null, null, null, null
        );

        TripExpenseEntity entity = tripExpenseMapper.toEntity(expense, tripEntity);

        assertAll(
                () -> assertEquals(3L, entity.getId()),
                () -> assertSame(tripEntity, entity.getTrip()),
                () -> assertEquals(ExpenseCategory.FOOD, entity.getCategory()),
                () -> assertEquals(new BigDecimal("42.50"), entity.getAmount()),
                () -> assertEquals("Almoço", entity.getDescription()),
                () -> assertEquals(createdAt, entity.getCreatedAt())
        );
    }

    @Test
    void shouldMapEntityToDomainWithTripId() {
        Instant createdAt = Instant.parse("2026-09-14T12:00:00Z");
        TripEntity tripEntity = new TripEntity(
                42L, "A", "B", 10.0, null, null, null, null, null, null
        );
        TripExpenseEntity entity = new TripExpenseEntity(
                3L, tripEntity, ExpenseCategory.FOOD, new BigDecimal("42.50"), "Almoço", createdAt
        );

        TripExpense expense = tripExpenseMapper.toDomain(entity);

        assertAll(
                () -> assertEquals(3L, expense.getId()),
                () -> assertEquals(42L, expense.getTripId()),
                () -> assertEquals(ExpenseCategory.FOOD, expense.getCategory()),
                () -> assertEquals(new BigDecimal("42.50"), expense.getAmount()),
                () -> assertEquals("Almoço", expense.getDescription()),
                () -> assertEquals(createdAt, expense.getCreatedAt())
        );
    }
}
