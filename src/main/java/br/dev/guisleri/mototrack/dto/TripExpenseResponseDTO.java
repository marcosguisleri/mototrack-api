package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.TripExpense;

import java.math.BigDecimal;
import java.time.Instant;

public record TripExpenseResponseDTO(
        long id,
        ExpenseCategory category,
        BigDecimal amount,
        String description,
        Instant createdAt
) {
    public static TripExpenseResponseDTO from(TripExpense tripExpense) {
        return new TripExpenseResponseDTO(
                tripExpense.getId(),
                tripExpense.getCategory(),
                tripExpense.getAmount(),
                tripExpense.getDescription(),
                tripExpense.getCreatedAt()
        );
    }
}
