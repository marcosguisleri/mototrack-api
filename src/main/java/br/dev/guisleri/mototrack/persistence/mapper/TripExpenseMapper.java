package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripExpenseEntity;
import org.springframework.stereotype.Component;

@Component
public class TripExpenseMapper {

    public TripExpenseEntity toEntity(
            TripExpense tripExpense,
            TripEntity tripEntity
    ) {
        return new TripExpenseEntity(
                tripExpense.getId(),
                tripEntity,
                tripExpense.getCategory(),
                tripExpense.getAmount(),
                tripExpense.getDescription(),
                tripExpense.getCreatedAt()
        );
    }

    public TripExpense toDomain(TripExpenseEntity tripExpenseEntity) {
        return TripExpense.restore(
                tripExpenseEntity.getId(),
                tripExpenseEntity.getTrip().getId(),
                tripExpenseEntity.getCategory(),
                tripExpenseEntity.getAmount(),
                tripExpenseEntity.getDescription(),
                tripExpenseEntity.getCreatedAt()
        );
    }

}
