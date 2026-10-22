package br.dev.guisleri.mototrack.persistence.projection;

import br.dev.guisleri.mototrack.model.ExpenseCategory;

import java.math.BigDecimal;

public interface ExpenseCategoryCostProjection {

    ExpenseCategory getCategory();

    Long getExpenseCount();

    BigDecimal getTotal();

}
