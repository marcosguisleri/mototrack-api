package br.dev.guisleri.mototrack.persistence.mapper;

import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void shouldMapUserToEntity() {
        User user = User.register("Marcos", "marcos@example.com");

        UserEntity entity = mapper.toEntity(user);

        assertAll(
                () -> assertNull(user.getId()),
                () -> assertNull(entity.getId()),
                () -> assertEquals("Marcos", entity.getName()),
                () -> assertEquals("marcos@example.com", entity.getEmail())
        );
    }

    @Test
    void shouldMapUserEntityToDomain() {
        UserEntity entity = new UserEntity(
                1L,
                "Marcos",
                "marcos@example.com"
        );

        User user = mapper.toDomain(entity);

        assertAll(
                () -> assertEquals(1L, user.getId()),
                () -> assertEquals("Marcos", user.getName()),
                () -> assertEquals("marcos@example.com", user.getEmail())
        );
    }
}
