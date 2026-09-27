package br.dev.guisleri.mototrack.persistence.projection;

import br.dev.guisleri.mototrack.model.TerrainType;

public interface TerrainTripStatisticsProjection {

    TerrainType getTerrain();

    Long getTripCount();

    Double getTotalDistanceKm();

}
