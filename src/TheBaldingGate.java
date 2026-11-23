import java.util.List;
import java.util.Scanner;

/** Small console entry point for trying the class and task systems. */
public class TheBaldingGate {
    private static final List<CombatClass> CLASSES = List.of(
            new Warrior(), new Mage(), new Frostmancer(), new Ranger(), new Paladin());

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Choose your class:");
            for (int i = 0; i < CLASSES.size(); i++) {
                System.out.printf("%d. %s - %s%n", i + 1, CLASSES.get(i).getName(), CLASSES.get(i).getDescription());
            }
            int choice = readChoice(input, 1, CLASSES.size());
            CombatClass playerClass = CLASSES.get(choice - 1);
            PlayerProgress progress = new PlayerProgress(playerClass);
            TaskBoard tasks = new TaskBoard();
            System.out.println("Selected " + playerClass.getName() + ". Starting currency: " + progress.getCurrency());
            System.out.println("Tasks can be completed once. Their rewards become point-buy currency.");

            while (true) {
                System.out.println("\nCurrency: " + progress.getCurrency() + " | Point-buy points: " + progress.getUnspentPoints());
                System.out.println("1. View tasks  2. Complete a task  3. Spend points  4. View character  0. Exit");
                int action = readChoice(input, 0, 4);
                if (action == 0) break;
                if (action == 1) {
                    for (Task task : tasks.getTasks()) {
                        System.out.printf("%2d. [%s] %s - %s (%d currency)%n", task.getId(),
                                task.isCompleted() ? "DONE" : "OPEN", task.getTitle(), task.getDescription(), task.getReward());
                    }
                } else if (action == 2) {
                    System.out.print("Task number to complete: ");
                    int id = readChoice(input, 1, tasks.getTasks().size());
                    Task task = tasks.completeTask(id);
                    if (task == null) System.out.println("That task is already complete.");
                    else {
                        progress.earnCurrency(task.getReward());
                        System.out.println("Completed: " + task.getTitle() + ". Earned " + task.getReward() + " currency.");
                    }
                } else if (action == 3) {
                    System.out.println("Point-buy: 1 point costs " + progress.getPointCost() + " currency.");
                    System.out.print("How many points to buy? ");
                    int amount = readChoice(input, 1, 1000);
                    if (progress.buyPoints(amount)) System.out.println("Purchased " + amount + " point(s).");
                    else System.out.println("Not enough currency.");
                } else {
                    System.out.println(progress.getCombatClass().getName() + " | HP " + progress.getMaxHealth()
                            + " | Attack " + progress.getAttack() + " | Defense " + progress.getDefense()
                            + " | unspent points " + progress.getUnspentPoints());
                }
            }
        }
    }

    private static int readChoice(Scanner input, int min, int max) {
        while (true) {
            System.out.printf("Choose %d-%d: ", min, max);
            if (input.hasNextInt()) {
                int value = input.nextInt();
                if (value >= min && value <= max) return value;
            } else input.next();
            System.out.println("Enter a number in range.");
        }
    }
}
