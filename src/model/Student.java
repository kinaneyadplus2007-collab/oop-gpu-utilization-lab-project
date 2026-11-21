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
        return Lines.join(
                Lines.enc(getUsername()),
                Lines.enc(getName()),
                Lines.enc(email),
                Lines.enc(phone),
                Lines.enc(getPassword()),
                String.valueOf(allowance.getAllocated()),
                String.valueOf(allowance.getUsed()),
                String.valueOf(allowance.getReserved())
        );
    }

    public static Student fromLine(String line) throws RuleException {
        String[] f = Lines.split(line, 8);

        try {
            TimeAllowance saved = new TimeAllowance(
                    Integer.parseInt(f[5]),
                    Integer.parseInt(f[6]),
                    Integer.parseInt(f[7])
            );

            return new Student(
                    Lines.dec(f[0]),
                    Lines.dec(f[1]),
                    Lines.dec(f[2]),
                    Lines.dec(f[3]),
                    Lines.dec(f[4]),
                    saved
            );
        } catch (NumberFormatException e) {
            throw new RuleException("Bad saved student line.");
        }
    }
}