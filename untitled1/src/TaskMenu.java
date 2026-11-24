import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

/** Array-based console menus for viewing, choosing, and attempting tasks. */
public class TaskMenu {
    private static final int SAFE = 0;
    private static final int VARIABLE = 1;
    private static final int HIGH_RISK = 2;
    private static final String[] SECTION_LABELS = {
            "===== SAFE TASKS: guaranteed reward, one attempt each =====",
            "===== VARIABLE REWARD TASKS: every roll pays, can be completed twice =====",
            "===== HIGH RISK TASKS: fail and earn nothing, 3 tries to succeed once ====="
    };

    private final TaskBoard board;
    private final PlayerProgress progress;

    public TaskMenu(TaskBoard board, PlayerProgress progress) {
        if (board == null || progress == null) throw new IllegalArgumentException("board and progress are required");
        this.board = board;
        this.progress = progress;
    }

    /** Lists every task by section, including tasks with no attempts left. */
    public void showTasks() {
        System.out.println("Task attempts used: " + board.getAttemptsUsed() + "/" + TaskBoard.REQUIRED_ATTEMPTS);
        for (int section = SAFE; section <= HIGH_RISK; section++) {
            System.out.println("\n" + SECTION_LABELS[section]);
            for (Task task : board.getTasks()) {
                if (sectionOf(task) == section) {
                    System.out.printf("  [%s] %s - %s (%s)%n", task.isCompleted() ? "DONE" : "OPEN",
                            task.getTitle(), task.getDescription(), rewardLabel(task));
                }
            }
        }
    }

    /** Shows one numbered list of available tasks under section labels, then attempts the selection. */
    public void chooseTask(Scanner input) {
        if (board.isFinished()) {
            System.out.println("This character has already attempted " + TaskBoard.REQUIRED_ATTEMPTS + " tasks.");
            return;
        }
        System.out.println("\nTask attempts left: " + board.getAttemptsRemaining());
        Task[] options = availableTasks();
        int currentSection = -1;
        for (int i = 0; i < options.length; i++) {
            if (sectionOf(options[i]) != currentSection) {
                currentSection = sectionOf(options[i]);
                System.out.println("\n" + SECTION_LABELS[currentSection]);
            }
            System.out.printf("%2d. %s - %s (%s)%n", i + 1, options[i].getTitle(),
                    options[i].getDescription(), rewardLabel(options[i]));
        }
        System.out.println("\n 0. Back");
        int selection = TheBaldingGate.readChoice(input, 0, options.length);
        if (selection != 0) attempt(options[selection - 1]);
    }

    private void attempt(Task task) {
        TaskBoard.AttemptResult result = board.attemptTask(task.getId());
        if (!task.isGuaranteed()) {
            System.out.println("d20 roll: " + task.getLastRoll() + "."
                    + (task.isLastRollCritical() ? " CRITICAL WIN!" : ""));
        }
        if (result == TaskBoard.AttemptResult.SUCCESS) {
            progress.earnCurrency(task.getLastReward());
            System.out.println("Success: " + task.getTitle() + ". Earned " + task.getLastReward() + " currency.");
        } else {
            System.out.println("Failed: " + task.getTitle() + ". You needed " + task.getTargetRoll() + " or higher.");
        }
        if (board.isFinished()) System.out.println("All " + TaskBoard.REQUIRED_ATTEMPTS + " task attempts are used.");
    }

    private Task[] availableTasks() {
        Task[] available = board.getTasks().stream().filter(Task::canAttempt).toArray(Task[]::new);
        Arrays.sort(available, Comparator.comparingInt(TaskMenu::sectionOf));
        return available;
    }

    private static int sectionOf(Task task) {
        if (task.isGuaranteed()) return SAFE;
        return task.isTiered() ? VARIABLE : HIGH_RISK;
    }

    private static String rewardLabel(Task task) {
        return (task.isTiered() ? "up to " : "") + task.getReward() + " currency";
    }
}
