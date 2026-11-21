package logic;

import java.time.LocalDateTime;
import java.util.*;
import model.AccessRequest;
import model.Administrator;
import model.RequestStatus;
import model.RuleException;
import model.Student;
import model.TimeGrant;
import model.User;

public class AccountService {
    public static final int DEFAULT_MINUTES = 100;

    private final Administrator admin = new Administrator("admin", "Administrator", "gpu2026");
    private final Map<String, Student> students = new HashMap<>();
    private final List<AccessRequest> requests = new ArrayList<>();
    private final List<TimeGrant> grants = new ArrayList<>();

    public AccessRequest register(String id, String name, String email, String phone, String password) throws RuleException {
        if (!Validator.notBlank(id) || !Validator.notBlank(name) || !Validator.notBlank(email) || !Validator.notBlank(phone) || !Validator.notBlank(password)) {
            throw new RuleException("Please fill in every field.");
        }
        id = id.trim();
        if (!Validator.validEmail(email)) {
            throw new RuleException("Enter a valid email address.");
        }
        if (!Validator.validPhone(phone)) {
            throw new RuleException("Enter a valid phone number (digits only, 7 to 15 digits).");
        }
        if (id.equals(admin.getUsername()) || students.containsKey(id)) {
            throw new RuleException("The university ID " + id + " is already registered.");
        }
        for (AccessRequest r : requests) {
            if (r.getUniversityId().equals(id) && r.getStatus() == RequestStatus.PENDING) {
                throw new RuleException("A request for " + id + " is already waiting for approval.");
            }
        }
        AccessRequest request = new AccessRequest(id, name.trim(), email.trim(), phone.trim(), password);
        requests.add(request);
        return request;
    }

    public User login(String username, String password) throws RuleException {
        if (!Validator.notBlank(username) || password == null || password.isEmpty()) {
            throw new RuleException("Enter your username and password.");
        }
        username = username.trim();
        if (admin.getUsername().equals(username)) {
            if (admin.checkPassword(password)) {
                return admin;
            }
            throw new RuleException("Wrong username or password.");
        }
        Student student = students.get(username);
        if (student != null) {
            if (student.checkPassword(password)) {
                return student;
            }
            throw new RuleException("Wrong username or password.");
        }
        AccessRequest latest = latestRequestOf(username);
        if (latest != null && latest.getStatus() == RequestStatus.PENDING) {
            throw new RuleException("Your registration request is still pending approval.");
        }
        if (latest != null && latest.getStatus() == RequestStatus.REJECTED) {
            throw new RuleException("Your registration request was rejected.");
        }
        throw new RuleException("Wrong username or password.");
    }

    public Student approve(AccessRequest request, int minutes) throws RuleException {
        if (minutes < 0) {
            throw new RuleException("The initial GPU minutes cannot be negative.");
        }
        if (students.containsKey(request.getUniversityId())) {
            throw new RuleException("The university ID " + request.getUniversityId() + " already has an account.");
        }
        request.approve();
        Student student = new Student(request.getUniversityId(), request.getName(), request.getEmail(), request.getPhone(), request.getPassword(), minutes);
        students.put(student.getUsername(), student);
        return student;
    }

    public void reject(AccessRequest request) throws RuleException {
        request.reject();
    }

    public void grantTime(Student student, int minutes, String reason, String adminName, LocalDateTime now) throws RuleException {
        if (!Validator.notBlank(reason)) {
            throw new RuleException("A reason is required to grant extra time.");
        }
        student.getAllowance().grant(minutes);
        grants.add(new TimeGrant(student.getUsername(), minutes, reason.trim(), adminName, now));
    }

    public Student findStudent(String id) {
        return (id == null) ? null : students.get(id.trim());
    }

    public Administrator getAdmin() {
        return admin;
    }

    public List<Student> getStudents() {
        List<Student> list = new ArrayList<>(students.values());
        list.sort((a, b) -> a.getUsername().compareTo(b.getUsername()));
        return list;
    }

    public List<AccessRequest> getRequests() {
        return new ArrayList<>(requests);
    }

    public List<TimeGrant> getGrants() {
        return new ArrayList<>(grants);
    }

    public void restore(List<Student> savedStudents, List<AccessRequest> savedRequests, List<TimeGrant> savedGrants) {
        students.clear();
        for (Student s : savedStudents) {
            students.put(s.getUsername(), s);
        }
        requests.clear();
        requests.addAll(savedRequests);
        grants.clear();
        grants.addAll(savedGrants);
    }

    private AccessRequest latestRequestOf(String id) {
        AccessRequest latest = null;
        for (AccessRequest r : requests) {
            if (r.getUniversityId().equals(id)) {
                latest = r;
            }
        }
        return latest;
    }
}
