package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MotorcycleMapperTest {

    private final UserMapper userMapper = new UserMapper();
    private final MotorcycleMapper mapper = new MotorcycleMapper(userMapper);

    @Test
    void shouldMapMotorcycleToEntityWithOwner() {
        User owner = User.restore(
                1L,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        Motorcycle motorcycle = Motorcycle.restore(
                10L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                owner
        );
        UserEntity ownerEntity = userMapper.toEntity(owner);

        MotorcycleEntity entity = mapper.toEntity(motorcycle, ownerEntity);

        assertAll(
                () -> assertEquals(10L, entity.getId()),
                () -> assertEquals("Honda", entity.getBrand()),
                () -> assertEquals("NX 500", entity.getModel()),
                () -> assertEquals("Black", entity.getColor()),
                () -> assertEquals(2025, entity.getYear()),
                () -> assertEquals(471, entity.getEngineCapacity()),
                () -> assertSame(ownerEntity, entity.getOwner())
        );
    }

    @Test
    void shouldMapMotorcycleEntityToDomainWithOwner() {
        UserEntity ownerEntity = new UserEntity(
                1L,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        MotorcycleEntity entity = new MotorcycleEntity(
                10L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                ownerEntity
        );

        Motorcycle motorcycle = mapper.toDomain(entity);

        assertAll(
                () -> assertEquals(10L, motorcycle.getId()),
                () -> assertEquals("Honda", motorcycle.getBrand()),
                () -> assertEquals("NX 500", motorcycle.getModel()),
                () -> assertEquals("Black", motorcycle.getColor()),
                () -> assertEquals(2025, motorcycle.getYear()),
                () -> assertEquals(471, motorcycle.getEngineCapacity()),
                () -> assertEquals(1L, motorcycle.getOwner().getId()),
                () -> assertEquals("Marcos", motorcycle.getOwner().getName()),
                () -> assertEquals(
                        "marcos@example.com",
                        motorcycle.getOwner().getEmail()
                )
        );
    }
}
