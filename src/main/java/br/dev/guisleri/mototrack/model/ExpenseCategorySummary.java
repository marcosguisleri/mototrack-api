package br.dev.guisleri.mototrack.model;

import java.math.BigDecimal;

public record ExpenseCategorySummary(
        ExpenseCategory category,
        long count,
        BigDecimal total
) {
}
