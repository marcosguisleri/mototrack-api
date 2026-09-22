package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.UserAlreadyExistsException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(
            String name,
            String email
    ) {

        String normalizedEmail = normalizeEmail(email);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Já existe um usuário cadastrado com este e-mail."
            );
        }

        User user = User.register(name, normalizedEmail);

        return userRepository.save(user);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id %d não encontrado".formatted(userId)
                ));
    }

    public User findUserByEmail(String userEmail) {
        String normalizedEmail = normalizeEmail(userEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com email: %s não encontrado".formatted(normalizedEmail)
                ));
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

}