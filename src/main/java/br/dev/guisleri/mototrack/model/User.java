package br.dev.guisleri.mototrack.model;

public class User {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;

    private User(Long id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static User register(
            String name,
            String email,
            String passwordHash
    ) {
        return new User(null, name, email, passwordHash);
    }

    public static User restore(
            Long id,
            String name,
            String email,
            String passwordHash
    ) {
        return new User(id, name, email, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
