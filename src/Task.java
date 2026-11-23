/** A one-time objective that awards currency when completed. */
public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final int reward;
    private boolean completed;

    public Task(int id, String title, String description, int reward) {
        if (id < 1 || reward < 0) throw new IllegalArgumentException("Invalid task id or reward");
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getReward() { return reward; }
    public boolean isCompleted() { return completed; }
    boolean markCompleted() {
        if (completed) return false;
        completed = true;
        return true;
    }
}
