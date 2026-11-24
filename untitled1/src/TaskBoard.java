import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One character's catalog of 30 town tasks, of which that character attempts 5 before battle. */
public class TaskBoard {
    public static final int REQUIRED_ATTEMPTS = 5;

    public enum AttemptResult { SUCCESS, FAILED, NO_TASK_ATTEMPTS_LEFT, BOARD_FINISHED }

    private final List<Task> tasks = new ArrayList<>();
    private int attemptsUsed;

    public TaskBoard() {
        // Guaranteed: always succeed, lower rewards, cannot be repeated.
        addGuaranteed("Sweep the Tavern", "Clear last night's crumbs from the tavern floor.", 60);
        addGuaranteed("Fetch Water", "Haul fresh water up from the village well.", 65);
        addGuaranteed("Feed the Horses", "Fill the troughs at the town stables.", 70);
        addGuaranteed("Stack Firewood", "Restock the woodpile behind the inn.", 80);
        addGuaranteed("Deliver a Letter", "Carry a sealed message to the town clerk.", 90);
        addGuaranteed("Wash the Laundry", "Scrub and hang the innkeeper's linens.", 95);
        addGuaranteed("Gather Herbs", "Pick herbs for the local apothecary.", 100);
        addGuaranteed("Weed the Garden", "Clear the weeds from a neighbor's vegetable patch.", 115);
        addGuaranteed("Repair the Fence", "Mend the broken fence around the sheep pasture.", 125);
        addGuaranteed("Tidy the Cellar", "Organize the crates in the general store's cellar.", 130);
        addGuaranteed("Help the Baker", "Knead dough and carry flour before sunrise.", 140);
        addGuaranteed("Mind the Shop", "Watch the general store while the owner runs errands.", 150);
        addGuaranteed("Light the Street Lamps", "Walk the town at dusk and light every lamp.", 160);
        addGuaranteed("Assist the Blacksmith", "Work the bellows and haul coal at the forge.", 175);

        // Pass/fail tasks need a target d20 roll and lock after one success. Tiered tasks pay by roll, twice.
        addRisky("Rescue the Cat", "Coax a stubborn cat down from the apple tree.", 240, 6);
        addRisky("Catch the Runaway Goat", "Chase the farmer's goat back through the market.", 260, 6);
        addTiered("Wait Tables", "Serve the tavern's dinner rush and keep your tips.",
                tier(1, 50), tier(8, 200), tier(15, 350), tier(20, 525));
        addRisky("Carry the Eggs", "Get a full basket to market without breaking one.", 290, 7);
        addRisky("Shoe the Mule", "Fit new shoes on the stable's most ornery mule.", 310, 7);
        addRisky("Thatch the Inn Roof", "Patch the leaks before the next rainfall.", 360, 9);
        addRisky("Balance the Ledger", "Find the missing coins in the shopkeeper's accounts.", 385, 9);
        addTiered("Busk in the Square", "Play music for the crowd and collect their coins.",
                tier(1, 45), tier(8, 225), tier(15, 400), tier(20, 600));
        addRisky("Unclog the Well", "Clear the blockage from the town well.", 410, 10);
        addTiered("Run a Market Stall", "Sell a merchant's goods for a share of the profit.",
                tier(1, 55), tier(9, 250), tier(16, 450), tier(20, 700));
        addTiered("Fish the Mill Pond", "Sell whatever you catch to the town cook.",
                tier(1, 40), tier(10, 200), tier(16, 450), tier(20, 725));
        addRisky("Fix the Mill Wheel", "Replace the cracked paddles on the grain mill.", 500, 12);
        addTiered("Harvest the Orchard", "Get paid by the bushel for the apples you pick.",
                tier(1, 25), tier(8, 150), tier(14, 350), tier(18, 625), tier(20, 875));
        addRisky("Repair the Clock Tower", "Get the town's stopped clock ticking again.", 625, 14);
        addRisky("Bake the Festival Cake", "Impress the mayor at the harvest festival.", 750, 16);
        addRisky("Polish the Balding Gate", "Buff the town's famous gate until it gleams.", 1200, 17);
    }

    private void addGuaranteed(String title, String description, int reward) {
        tasks.add(new Task(tasks.size() + 1, title, description, reward));
    }

    private void addRisky(String title, String description, int reward, int targetRoll) {
        tasks.add(new Task(tasks.size() + 1, title, description, reward, targetRoll));
    }

    private void addTiered(String title, String description, Task.RewardTier... tiers) {
        tasks.add(new Task(tasks.size() + 1, title, description, tiers));
    }

    private static Task.RewardTier tier(int minRoll, int reward) {
        return new Task.RewardTier(minRoll, reward);
    }

    public List<Task> getTasks() { return Collections.unmodifiableList(tasks); }
    public int getAttemptsUsed() { return attemptsUsed; }
    public int getAttemptsRemaining() { return REQUIRED_ATTEMPTS - attemptsUsed; }
    public boolean isFinished() { return attemptsUsed >= REQUIRED_ATTEMPTS; }

    /** Restores saved attempts and successes for every task. */
    void restore(int[] attempts, int[] successes) {
        if (attempts.length != tasks.size() || successes.length != tasks.size()) {
            throw new IllegalArgumentException("Saved task count does not match");
        }
        int total = 0;
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).restore(attempts[i], successes[i]);
            total += attempts[i];
        }
        if (total > REQUIRED_ATTEMPTS) throw new IllegalArgumentException("Too many saved task attempts");
        attemptsUsed = total;
    }

    /** Attempts a task. Blocked attempts do not count toward the character's 5. */
    public AttemptResult attemptTask(int id) {
        if (id < 1 || id > tasks.size()) throw new IllegalArgumentException("Unknown task id: " + id);
        if (isFinished()) return AttemptResult.BOARD_FINISHED;
        Task task = tasks.get(id - 1);
        if (!task.canAttempt()) return AttemptResult.NO_TASK_ATTEMPTS_LEFT;
        attemptsUsed++;
        return task.attempt() ? AttemptResult.SUCCESS : AttemptResult.FAILED;
    }
}
