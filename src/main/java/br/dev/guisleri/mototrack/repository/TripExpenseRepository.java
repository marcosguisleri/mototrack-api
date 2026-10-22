package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.TripExpense;

import java.util.List;
import java.util.Optional;

public interface TripExpenseRepository {

    TripExpense save(TripExpense tripExpense);

    List<TripExpense> findByTripId(long tripId);

    Optional<TripExpense> findByIdAndTripIdForUpdate(long expenseId, long tripId);

    void deleteById(Long expenseId);

    void deleteByTripId(Long tripId);

}
