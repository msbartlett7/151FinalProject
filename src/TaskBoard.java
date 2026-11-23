import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selectable catalog of 15 one-time tasks. */
public class TaskBoard {
    private final List<Task> tasks = new ArrayList<>();

    public TaskBoard() {
        add("First Blood", "Win your first combat encounter.", 10);
        add("Still Standing", "Finish a battle with at least half your health.", 10);
        add("Heavy Hitter", "Deal 50 total damage in a battle.", 12);
        add("Untouchable", "Win a battle without taking damage.", 15);
        add("Close Call", "Win a battle while below 25% health.", 15);
        add("Three Victories", "Win three combat encounters.", 18);
        add("Monster Hunter", "Defeat a nonplayer enemy with 100 or more health.", 18);
        add("Tactician", "Win a battle using a defensive action.", 12);
        add("Class Training", "Win a battle using your class ability.", 15);
        add("Flawless Run", "Win two battles in a row without being defeated.", 20);
        add("Prepared", "Enter a battle with a purchased point-buy upgrade.", 12);
        add("Comeback", "Win after falling below half health during the fight.", 16);
        add("Veteran", "Win five combat encounters.", 25);
        add("Elite Slayer", "Defeat an elite nonplayer enemy.", 25);
        add("Champion", "Win a final or boss encounter.", 30);
    }

    private void add(String title, String description, int reward) {
        tasks.add(new Task(tasks.size() + 1, title, description, reward));
    }

    public List<Task> getTasks() { return Collections.unmodifiableList(tasks); }

    /** Returns the newly completed task, or null when it was already completed. */
    public Task completeTask(int id) {
        if (id < 1 || id > tasks.size()) throw new IllegalArgumentException("Unknown task id: " + id);
        Task task = tasks.get(id - 1);
        return task.markCompleted() ? task : null;
    }
}
