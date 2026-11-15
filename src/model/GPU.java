package model;

public class GPU {
    private final int id;
    private Job currentJob;

    public GPU(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Job getCurrentJob() {
        return currentJob;
    }

    public boolean isFree() {
        return currentJob == null;
    }

    public void assign(Job job) throws RuleException {
        if (!isFree()) {
            throw new RuleException("GPU" + id + " is busy with job " + currentJob.getJobId() + ".");
        }
        currentJob = job;
    }

    public void release() {
        currentJob = null;
    }
}