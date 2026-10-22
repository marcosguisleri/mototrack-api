package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripExpenseException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

public class TripExpense {

    private final Long id;
    private final Long tripId;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String description;
    private final Instant createdAt;

    private TripExpense(
            Long id,
            Long tripId,
            ExpenseCategory category,
            BigDecimal amount,
            String description,
            Instant createdAt
    ) {
        this.id = id;
        this.tripId = tripId;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static TripExpense register(
            Long tripId,
            ExpenseCategory category,
            BigDecimal amount,
            String description,
            Clock clock
    ) {
        validate(category, amount);

        return new TripExpense(
                null,
                tripId,
                category,
                amount,
                normalize(description),
                clock.instant()
        );
    }

    public static TripExpense restore(
            Long id,
            Long tripId,
            ExpenseCategory category,
            BigDecimal amount,
            String description,
            Instant createdAt
    ) {
        return new TripExpense(id, tripId, category, amount, description, createdAt);
    }

    public void update(
            ExpenseCategory category,
            BigDecimal amount,
            String description
    ) {
        validate(category, amount);

        this.category = category;
        this.amount = amount;
        this.description = normalize(description);
    }

    private static void validate(ExpenseCategory category, BigDecimal amount) {
        if (category == null) {
            throw new InvalidTripExpenseException(
                    "A categoria do gasto é obrigatória."
            );
        }

        if (amount.signum() < 0) {
            throw new InvalidTripExpenseException(
                    "O valor do gasto não pode ser negativo."
            );
        }

        // A toll-exempt passage (e.g. motorcycles) is recorded with amount zero.
        if (amount.signum() == 0 && category != ExpenseCategory.TOLL) {
            throw new InvalidTripExpenseException(
                    "Somente o pedágio pode ter valor zero (isento)."
            );
        }
    }

    private static String normalize(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
