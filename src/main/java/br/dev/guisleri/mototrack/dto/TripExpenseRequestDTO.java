package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TripExpenseRequestDTO(

        @NotNull(message = "A categoria do gasto é obrigatória.")
        ExpenseCategory category,

        @NotNull(message = "O valor do gasto é obrigatório.")
        @PositiveOrZero(message = "O valor do gasto não pode ser negativo.")
        @Digits(
                integer = 8,
                fraction = 2,
                message = "O valor do gasto deve ter até 8 dígitos inteiros e 2 casas decimais."
        )
        BigDecimal amount,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description
) {
}
