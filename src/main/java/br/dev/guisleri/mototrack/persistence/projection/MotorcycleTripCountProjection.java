package br.dev.guisleri.mototrack.persistence.projection;

import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;

public interface MotorcycleTripCountProjection {

    MotorcycleEntity getMotorcycle();

    Long getCount();

}
