package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FuelEntryRequestDTO(

        @NotNull(message = "Os litros abastecidos são obrigatórios.")
        @Positive(message = "Os litros abastecidos devem ser maiores que zero.")
        @Digits(
                integer = 4,
                fraction = 3,
                message = "Os litros devem ter até 4 dígitos inteiros e 3 casas decimais."
        )
        BigDecimal liters,

        @NotNull(message = "O valor pago é obrigatório.")
        @Positive(message = "O valor pago deve ser maior que zero.")
        @Digits(
                integer = 8,
                fraction = 2,
                message = "O valor pago deve ter até 8 dígitos inteiros e 2 casas decimais."
        )
        BigDecimal totalCost
) {
}
