package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.persistence.entity.TripExpenseEntity;
import br.dev.guisleri.mototrack.persistence.mapper.TripExpenseMapper;
import br.dev.guisleri.mototrack.repository.TripExpenseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaTripExpenseRepositoryAdapter implements TripExpenseRepository {

    private final SpringDataTripExpenseRepository springDataTripExpenseRepository;
    private final SpringDataTripRepository springDataTripRepository;
    private final TripExpenseMapper tripExpenseMapper;

    public JpaTripExpenseRepositoryAdapter(
            SpringDataTripExpenseRepository springDataTripExpenseRepository,
            SpringDataTripRepository springDataTripRepository,
            TripExpenseMapper tripExpenseMapper
    ) {
        this.springDataTripExpenseRepository = springDataTripExpenseRepository;
        this.springDataTripRepository = springDataTripRepository;
        this.tripExpenseMapper = tripExpenseMapper;
    }

    @Override
    @Transactional(readOnly = false)
    public TripExpense save(TripExpense tripExpense) {
        TripExpenseEntity tripExpenseEntity = tripExpenseMapper.toEntity(
                tripExpense,
                springDataTripRepository.getReferenceById(tripExpense.getTripId())
        );

        return tripExpenseMapper.toDomain(
                springDataTripExpenseRepository.save(tripExpenseEntity)
        );
    }

    @Override
    public List<TripExpense> findByTripId(long tripId) {
        return springDataTripExpenseRepository
                .findByTrip_IdOrderByCreatedAtAscIdAsc(tripId)
                .stream()
                .map(tripExpenseMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = false)
    public Optional<TripExpense> findByIdAndTripIdForUpdate(
            long expenseId,
            long tripId
    ) {
        return springDataTripExpenseRepository
                .findByIdAndTripIdForUpdate(expenseId, tripId)
                .map(tripExpenseMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long expenseId) {
        springDataTripExpenseRepository.deleteById(expenseId);
    }

    @Override
    @Transactional
    public void deleteByTripId(Long tripId) {
        springDataTripExpenseRepository.deleteByTripId(tripId);
    }

}
