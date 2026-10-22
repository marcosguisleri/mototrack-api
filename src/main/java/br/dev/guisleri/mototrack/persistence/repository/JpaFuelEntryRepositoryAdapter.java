package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.persistence.entity.FuelEntryEntity;
import br.dev.guisleri.mototrack.persistence.mapper.FuelEntryMapper;
import br.dev.guisleri.mototrack.repository.FuelEntryRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaFuelEntryRepositoryAdapter implements FuelEntryRepository {

    private final SpringDataFuelEntryRepository springDataFuelEntryRepository;
    private final SpringDataTripRepository springDataTripRepository;
    private final FuelEntryMapper fuelEntryMapper;

    public JpaFuelEntryRepositoryAdapter(
            SpringDataFuelEntryRepository springDataFuelEntryRepository,
            SpringDataTripRepository springDataTripRepository,
            FuelEntryMapper fuelEntryMapper
    ) {
        this.springDataFuelEntryRepository = springDataFuelEntryRepository;
        this.springDataTripRepository = springDataTripRepository;
        this.fuelEntryMapper = fuelEntryMapper;
    }

    @Override
    @Transactional(readOnly = false)
    public FuelEntry save(FuelEntry fuelEntry) {
        FuelEntryEntity fuelEntryEntity = fuelEntryMapper.toEntity(
                fuelEntry,
                springDataTripRepository.getReferenceById(fuelEntry.getTripId())
        );

        return fuelEntryMapper.toDomain(
                springDataFuelEntryRepository.save(fuelEntryEntity)
        );
    }

    @Override
    public List<FuelEntry> findByTripId(long tripId) {
        return springDataFuelEntryRepository
                .findByTrip_IdOrderByCreatedAtAscIdAsc(tripId)
                .stream()
                .map(fuelEntryMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = false)
    public Optional<FuelEntry> findByIdAndTripIdForUpdate(
            long fuelEntryId,
            long tripId
    ) {
        return springDataFuelEntryRepository
                .findByIdAndTripIdForUpdate(fuelEntryId, tripId)
                .map(fuelEntryMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long fuelEntryId) {
        springDataFuelEntryRepository.deleteById(fuelEntryId);
    }

    @Override
    @Transactional
    public void deleteByTripId(Long tripId) {
        springDataFuelEntryRepository.deleteByTripId(tripId);
    }

}
