package model;

public abstract class User {
    private final String username;
    private final String name;
    private final String password;

    protected User(String username, String name, String password) {
        this.username = username;
        this.name = name;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }

    public abstract boolean canCancel(Job job);

    public abstract String getRoleLabel();
}