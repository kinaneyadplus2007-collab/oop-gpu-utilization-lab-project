package model;

public class Student extends User implements Storable {
    private final String email;
    private final String phone;
    private final TimeAllowance allowance;

    public Student(String universityId, String name, String email, String phone,
                   String password, int allocatedMinutes) {
        this(universityId, name, email, phone, password, new TimeAllowance(allocatedMinutes));
    }

    public Student(String universityId, String name, String email, String phone,
                   String password, TimeAllowance allowance) {
        super(universityId, name, password);
        this.email = email;
        this.phone = phone;
        this.allowance = allowance;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public TimeAllowance getAllowance() {
        return allowance;
    }

    @Override
    public boolean canCancel(Job job) {
        return job.getStudentId().equals(getUsername());
    }

    @Override
    public String getRoleLabel() {
        return "Student";
    }

    @Override
    public String toLine() {
        throw new UnsupportedOperationException("TODO: agree the line format with Ali (DataStore)");
    }
}