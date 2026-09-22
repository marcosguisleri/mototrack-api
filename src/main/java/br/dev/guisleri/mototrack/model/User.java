package br.dev.guisleri.mototrack.model;

public class User {

    private final Long id;
    private final String name;
    private final String email;

    private User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static User register(
            String name,
            String email
    ) {
        return new User(null, name, email);
    }

    public static User restore(
            Long id,
            String name,
            String email
    ) {
        return new User(id, name, email);
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
}
