package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.User;

public record UserResponseDTO(
        Long id,
        String name,
        String email
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
