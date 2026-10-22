package br.dev.guisleri.mototrack.persistence.projection;

import java.math.BigDecimal;

public interface FuelCostProjection {

    Long getEntryCount();

    BigDecimal getLiters();

    BigDecimal getTotalCost();

}
