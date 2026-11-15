package model;

public class AccessRequest implements Storable {
    private final String universityId;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;
    private RequestStatus status = RequestStatus.PENDING;

    public AccessRequest(String universityId, String name, String email, String phone, String password) {
        this.universityId = universityId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public String getUniversityId() { return universityId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public RequestStatus getStatus() { return status; }

    public void approve() throws RuleException {
        requirePending("approved");
        status = RequestStatus.APPROVED;
    }

    public void reject() throws RuleException {
        requirePending("rejected");
        status = RequestStatus.REJECTED;
    }

    private void requirePending(String action) throws RuleException {
        if (status != RequestStatus.PENDING) {
            throw new RuleException("The request of " + universityId + " was already "
                    + status.name().toLowerCase() + " and cannot be " + action + " again.");
        }
    }

    @Override
    public String toLine() {
        throw new UnsupportedOperationException("TODO: agree the line format with Ali (DataStore)");
    }
}