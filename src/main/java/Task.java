public class Task {
    private final String taskName;
    private boolean completed;

    public Task(String taskName, boolean completed) {
        this.taskName = taskName;
        this.completed = completed;
    }

    public String getTaskName() {
        return taskName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }

    @Override
    public String toString() {
        return (completed ? "[Completed] " : "[Pending] ") + taskName;
    }
}
