package model;

public class Administrator extends User {
    public Administrator(String username, String name, String password) {
        super(username, name, password);
    }

    @Override
    public boolean canCancel(Job job) {
        return true;
    }

    @Override
    public String getRoleLabel() {
        return "Administrator";
    }
}