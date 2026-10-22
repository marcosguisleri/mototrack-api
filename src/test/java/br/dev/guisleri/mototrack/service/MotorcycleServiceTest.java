package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.MotorcycleAccessDeniedException;
import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.User;
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

import static org.junit.jupiter.api.Assertions.assertAll;
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

    private static final Long OWNER_ID = 1L;

    @Mock
    private MotorcycleRepository motorcycleRepository;

    @Mock
    private TripRepository tripRepository;

    private MotorcycleService service;

    @BeforeEach
    void setUp() {
        service = new MotorcycleService(motorcycleRepository, tripRepository);
    }

    // -------------------------------------------------------------------------
    // Registro de motocicleta
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterMotorcycleForOwner() {
        User owner = owner();
        Motorcycle persistedMotorcycle = motorcycle(1L, "Honda", "NX 500", owner);
        ArgumentCaptor<Motorcycle> motorcycleCaptor =
                ArgumentCaptor.forClass(Motorcycle.class);
        when(motorcycleRepository.save(any(Motorcycle.class)))
                .thenReturn(persistedMotorcycle);

        Motorcycle result = service.registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                MotorcycleType.NAKED,
                owner
        );

        verify(motorcycleRepository).save(motorcycleCaptor.capture());
        Motorcycle motorcycleSentToRepository = motorcycleCaptor.getValue();
        assertNull(motorcycleSentToRepository.getId());
        assertEquals("Honda", motorcycleSentToRepository.getBrand());
        assertEquals("NX 500", motorcycleSentToRepository.getModel());
        assertEquals("Black", motorcycleSentToRepository.getColor());
        assertEquals(2025, motorcycleSentToRepository.getYear());
        assertEquals(471, motorcycleSentToRepository.getEngineCapacity());
        assertEquals(MotorcycleType.NAKED, motorcycleSentToRepository.getType());
        assertSame(owner, motorcycleSentToRepository.getOwner());
        assertSame(persistedMotorcycle, result);
    }

    // -------------------------------------------------------------------------
    // Edição com lock e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateMotorcycleForOwner() {
        User owner = owner();
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", owner);
        when(motorcycleRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(motorcycle));
        when(motorcycleRepository.save(motorcycle)).thenReturn(motorcycle);

        Motorcycle result = service.updateMotorcycleForOwner(
                1L,
                OWNER_ID,
                "Triumph",
                "Tiger 1200",
                "Red",
                2026,
                1160,
                MotorcycleType.BIG_TRAIL
        );

        assertAll(
                () -> assertSame(motorcycle, result),
                () -> assertEquals(1L, result.getId()),
                () -> assertEquals("Triumph", result.getBrand()),
                () -> assertEquals("Tiger 1200", result.getModel()),
                () -> assertEquals("Red", result.getColor()),
                () -> assertEquals(2026, result.getYear()),
                () -> assertEquals(1160, result.getEngineCapacity()),
                () -> assertEquals(MotorcycleType.BIG_TRAIL, result.getType()),
                () -> assertSame(owner, result.getOwner())
        );
        verify(motorcycleRepository).findByIdForUpdate(1L);
        verify(motorcycleRepository).save(motorcycle);
    }

    @Test
    void shouldThrowWhenUpdatingUnknownMotorcycle() {
        when(motorcycleRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        MotorcycleNotFoundException exception = assertThrows(
                MotorcycleNotFoundException.class,
                () -> service.updateMotorcycleForOwner(
                        99L,
                        OWNER_ID,
                        "Triumph",
                        "Tiger 1200",
                        "Red",
                        2026,
                        1160,
                        MotorcycleType.BIG_TRAIL
                )
        );

        assertEquals("Motocicleta com id 99 não encontrada", exception.getMessage());
        verify(motorcycleRepository).findByIdForUpdate(99L);
        verify(motorcycleRepository, never()).save(any(Motorcycle.class));
    }

    @Test
    void shouldDenyUpdatingMotorcycleOwnedByAnotherUser() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", otherOwner());
        when(motorcycleRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(motorcycle));

        MotorcycleAccessDeniedException exception = assertThrows(
                MotorcycleAccessDeniedException.class,
                () -> service.updateMotorcycleForOwner(
                        1L,
                        OWNER_ID,
                        "Triumph",
                        "Tiger 1200",
                        "Red",
                        2026,
                        1160,
                        MotorcycleType.BIG_TRAIL
                )
        );

        assertEquals(
                "Você não possui permissão para acessar esta motocicleta.",
                exception.getMessage()
        );
        verify(motorcycleRepository).findByIdForUpdate(1L);
        verify(motorcycleRepository, never()).save(any(Motorcycle.class));
    }

    // -------------------------------------------------------------------------
    // Consulta individual e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldFindMotorcycleByIdForOwner() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", owner());
        when(motorcycleRepository.findById(1L))
                .thenReturn(Optional.of(motorcycle));

        Motorcycle result = service.findMotorcycleByIdForOwner(1L, OWNER_ID);

        assertSame(motorcycle, result);
        verify(motorcycleRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenFindingUnknownMotorcycleForOwner() {
        when(motorcycleRepository.findById(99L)).thenReturn(Optional.empty());

        MotorcycleNotFoundException exception = assertThrows(
                MotorcycleNotFoundException.class,
                () -> service.findMotorcycleByIdForOwner(99L, OWNER_ID)
        );

        assertEquals("Motocicleta com id 99 não encontrada", exception.getMessage());
        verify(motorcycleRepository).findById(99L);
    }

    @Test
    void shouldDenyFindingMotorcycleOwnedByAnotherUser() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", otherOwner());
        when(motorcycleRepository.findById(1L))
                .thenReturn(Optional.of(motorcycle));

        MotorcycleAccessDeniedException exception = assertThrows(
                MotorcycleAccessDeniedException.class,
                () -> service.findMotorcycleByIdForOwner(1L, OWNER_ID)
        );

        assertEquals(
                "Você não possui permissão para acessar esta motocicleta.",
                exception.getMessage()
        );
        verify(motorcycleRepository).findById(1L);
    }

    // -------------------------------------------------------------------------
    // Listagem e contagem da garagem do proprietário
    // -------------------------------------------------------------------------

    @Test
    void shouldFindMotorcyclesByOwnerId() {
        List<Motorcycle> motorcycles = List.of(
                motorcycle(1L, "Honda", "NX 500", owner()),
                motorcycle(2L, "Yamaha", "Tenere 700", owner())
        );
        when(motorcycleRepository.findByOwnerId(OWNER_ID)).thenReturn(motorcycles);

        List<Motorcycle> result = service.findMotorcyclesByOwnerId(OWNER_ID);

        assertSame(motorcycles, result);
        verify(motorcycleRepository).findByOwnerId(OWNER_ID);
    }

    @Test
    void shouldReturnEmptyGarageForOwnerWithoutMotorcycles() {
        when(motorcycleRepository.findByOwnerId(OWNER_ID)).thenReturn(List.of());

        List<Motorcycle> result = service.findMotorcyclesByOwnerId(OWNER_ID);

        assertEquals(List.of(), result);
        verify(motorcycleRepository).findByOwnerId(OWNER_ID);
    }

    @Test
    void shouldCountMotorcyclesByOwnerId() {
        when(motorcycleRepository.countByOwnerId(OWNER_ID)).thenReturn(3L);

        long result = service.countMotorcyclesByOwnerId(OWNER_ID);

        assertEquals(3L, result);
        verify(motorcycleRepository).countByOwnerId(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Exclusão com lock, vínculo com viagens e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteOwnersMotorcycleWithoutTrips() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", owner());
        when(motorcycleRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(motorcycle));
        when(tripRepository.existsByMotorcycleId(1L)).thenReturn(false);

        service.deleteMotorcycleByIdForOwner(1L, OWNER_ID);

        verify(motorcycleRepository).findByIdForUpdate(1L);
        verify(tripRepository).existsByMotorcycleId(1L);
        verify(motorcycleRepository).deleteById(1L);
    }

    @Test
    void shouldRejectDeletingOwnersMotorcycleWithTrips() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", owner());
        when(motorcycleRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(motorcycle));
        when(tripRepository.existsByMotorcycleId(1L)).thenReturn(true);

        MotorcycleInUseException exception = assertThrows(
                MotorcycleInUseException.class,
                () -> service.deleteMotorcycleByIdForOwner(1L, OWNER_ID)
        );

        assertEquals(
                "A motocicleta não pode ser excluída enquanto possuir viagens associadas.",
                exception.getMessage()
        );
        verify(motorcycleRepository).findByIdForUpdate(1L);
        verify(tripRepository).existsByMotorcycleId(1L);
        verify(motorcycleRepository, never()).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingUnknownMotorcycleForOwner() {
        when(motorcycleRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                MotorcycleNotFoundException.class,
                () -> service.deleteMotorcycleByIdForOwner(99L, OWNER_ID)
        );

        verify(motorcycleRepository).findByIdForUpdate(99L);
        verifyNoInteractions(tripRepository);
        verify(motorcycleRepository, never()).deleteById(99L);
    }

    @Test
    void shouldDenyDeletingMotorcycleOwnedByAnotherUser() {
        Motorcycle motorcycle = motorcycle(1L, "Honda", "NX 500", otherOwner());
        when(motorcycleRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(motorcycle));

        assertThrows(
                MotorcycleAccessDeniedException.class,
                () -> service.deleteMotorcycleByIdForOwner(1L, OWNER_ID)
        );

        verify(motorcycleRepository).findByIdForUpdate(1L);
        verifyNoInteractions(tripRepository);
        verify(motorcycleRepository, never()).deleteById(1L);
    }

    // -------------------------------------------------------------------------
    // Helpers de domínio
    // -------------------------------------------------------------------------

    private Motorcycle motorcycle(
            Long id,
            String brand,
            String model,
            User motorcycleOwner
    ) {
        return Motorcycle.restore(
                id,
                brand,
                model,
                "Black",
                2025,
                471,
                MotorcycleType.NAKED,
                motorcycleOwner
        );
    }

    private User owner() {
        return User.restore(
                OWNER_ID,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
    }

    private User otherOwner() {
        return User.restore(
                2L,
                "Ana",
                "ana@example.com",
                "password-hash"
        );
    }
}
