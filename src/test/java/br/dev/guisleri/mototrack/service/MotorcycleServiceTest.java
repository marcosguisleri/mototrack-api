package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.repository.MotorcycleRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotorcycleServiceTest {

    @Mock
    private MotorcycleRepository motorcycleRepository;

    @Mock
    private TripRepository tripRepository;

    private MotorcycleService service;

    @BeforeEach
    void setUp() {
        service = new MotorcycleService(motorcycleRepository, tripRepository);
    }

    @Test
    void shouldRegisterNewMotorcycleAndReturnPersistedMotorcycle() {
        Motorcycle persistedMotorcycle = motorcycle(1L, "Honda", "NX 500");
        ArgumentCaptor<Motorcycle> motorcycleCaptor =
                ArgumentCaptor.forClass(Motorcycle.class);
        when(motorcycleRepository.save(any(Motorcycle.class)))
                .thenReturn(persistedMotorcycle);

        Motorcycle result = service.registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );

        verify(motorcycleRepository).save(motorcycleCaptor.capture());
        Motorcycle motorcycleSentToRepository = motorcycleCaptor.getValue();
        assertNull(motorcycleSentToRepository.getId());
        assertEquals("Honda", motorcycleSentToRepository.getBrand());
        assertEquals("NX 500", motorcycleSentToRepository.getModel());
        assertEquals("Black", motorcycleSentToRepository.getColor());
        assertEquals(2025, motorcycleSentToRepository.getYear());
        assertEquals(471, motorcycleSentToRepository.getEngineCapacity());
        assertSame(persistedMotorcycle, result);
    }

    @Test
    void shouldFindMotorcycleById() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleRepository.findById(1L))
                .thenReturn(Optional.of(motorcycle));

        Motorcycle result = service.findMotorcycleById(1L);

        assertSame(motorcycle, result);
        verify(motorcycleRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenFindingUnknownMotorcycle() {
        when(motorcycleRepository.findById(99L)).thenReturn(Optional.empty());

        MotorcycleNotFoundException exception = assertThrows(
                MotorcycleNotFoundException.class,
                () -> service.findMotorcycleById(99L)
        );

        assertEquals("Motocicleta com id 99 não encontrada", exception.getMessage());
        verify(motorcycleRepository).findById(99L);
    }

    @Test
    void shouldFindAllMotorcycles() {
        List<Motorcycle> motorcycles = List.of(
                motorcycle(1L, "Honda", "NX 500"),
                motorcycle(2L, "Yamaha", "Tenere 700")
        );
        when(motorcycleRepository.findAll()).thenReturn(motorcycles);

        List<Motorcycle> result = service.findAllMotorcycles();

        assertSame(motorcycles, result);
        verify(motorcycleRepository).findAll();
    }

    @Test
    void shouldDeleteMotorcycleWithoutTrips() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleRepository.findById(1L))
                .thenReturn(Optional.of(motorcycle));
        when(tripRepository.existsByMotorcycleId(1L)).thenReturn(false);

        service.deleteMotorcycleById(1L);

        verify(motorcycleRepository).findById(1L);
        verify(tripRepository).existsByMotorcycleId(1L);
        verify(motorcycleRepository).deleteById(1L);
    }

    @Test
    void shouldRejectDeletingMotorcycleWithTrips() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500");
        when(motorcycleRepository.findById(1L))
                .thenReturn(Optional.of(motorcycle));
        when(tripRepository.existsByMotorcycleId(1L)).thenReturn(true);

        MotorcycleInUseException exception = assertThrows(
                MotorcycleInUseException.class,
                () -> service.deleteMotorcycleById(1L)
        );

        assertEquals(
                "A motocicleta não pode ser excluída enquanto possuir viagens associadas.",
                exception.getMessage()
        );
        verify(tripRepository).existsByMotorcycleId(1L);
        verify(motorcycleRepository, never()).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingUnknownMotorcycle() {
        when(motorcycleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                MotorcycleNotFoundException.class,
                () -> service.deleteMotorcycleById(99L)
        );

        verifyNoInteractions(tripRepository);
        verify(motorcycleRepository, never()).deleteById(99L);
    }

    private Motorcycle motorcycle(Long id, String brand, String model) {
        return Motorcycle.restore(id, brand, model, "Black", 2025, 471);
    }
}
