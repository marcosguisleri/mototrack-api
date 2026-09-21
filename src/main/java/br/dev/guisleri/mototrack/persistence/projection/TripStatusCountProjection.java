package br.dev.guisleri.mototrack.persistence.projection;

import br.dev.guisleri.mototrack.model.TripStatus;

public interface TripStatusCountProjection {

    TripStatus getStatus();

    Long getTripCount();

}
