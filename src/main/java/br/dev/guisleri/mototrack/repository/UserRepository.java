package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

}
