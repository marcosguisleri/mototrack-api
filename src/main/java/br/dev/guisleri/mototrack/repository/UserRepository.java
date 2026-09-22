package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

}
