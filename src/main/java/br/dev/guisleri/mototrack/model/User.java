package br.dev.guisleri.mototrack.model;

public class User {

    private final Long id;
    private String name;
    private String email;
    private String passwordHash;

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

    public void updateName(String name) {

        if (name == null || name.isBlank())
            return;

        this.name = name.trim();
    }

    public void changeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }

        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public void changePasswordHash(String passwordHash) {

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("O hash da senha é obrigatório.");
        }

        this.passwordHash = passwordHash;
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
