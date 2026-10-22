package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripExpenseNotFoundException;
import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.repository.TripExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
public class TripExpenseService {

    private final TripExpenseRepository tripExpenseRepository;
    private final TripService tripService;
    private final Clock clock;

    public TripExpenseService(
            TripExpenseRepository tripExpenseRepository,
            TripService tripService,
            Clock clock
    ) {
        this.tripExpenseRepository = tripExpenseRepository;
        this.tripService = tripService;
        this.clock = clock;
    }

    public TripExpense registerExpenseForOwner(
            long tripId,
            Long ownerId,
            ExpenseCategory category,
            BigDecimal amount,
            String description
    ) {
        Trip trip = tripService.findTripByIdForOwner(tripId, ownerId);
        trip.assertAcceptsRecords();

        return tripExpenseRepository.save(
                TripExpense.register(tripId, category, amount, description, clock)
        );
    }

    public List<TripExpense> findExpensesByTripIdForOwner(
            long tripId,
            Long ownerId
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);

        return tripExpenseRepository.findByTripId(tripId);
    }

    @Transactional
    public TripExpense updateExpenseForOwner(
            long tripId,
            long expenseId,
            Long ownerId,
            ExpenseCategory category,
            BigDecimal amount,
            String description
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);
        TripExpense expense = findExpenseForUpdate(expenseId, tripId);

        expense.update(category, amount, description);

        return tripExpenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpenseForOwner(
            long tripId,
            long expenseId,
            Long ownerId
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);
        findExpenseForUpdate(expenseId, tripId);

        tripExpenseRepository.deleteById(expenseId);
    }

    private TripExpense findExpenseForUpdate(long expenseId, long tripId) {
        return tripExpenseRepository.findByIdAndTripIdForUpdate(expenseId, tripId)
                .orElseThrow(() -> new TripExpenseNotFoundException(
                        "Gasto com id %d não encontrado".formatted(expenseId)
                ));
    }

}
