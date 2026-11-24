import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/** Console entry point: town tasks earn currency, currency buys gear, and characters can be saved. */
public class TheBaldingGate {
    static final List<CombatClass> CLASSES = List.of(
            new Warrior(), new Mage(), new Frostmancer(), new Ranger(), new Paladin());
    private static final String[] START_OPTIONS = {"New character", "Load character"};
    private static final String[] MENU_OPTIONS = {"View tasks", "Attempt a task", "Buy gear", "View character", "Save character"};
    private static final String[] EXIT_OPTIONS = {"Save and exit", "Exit without saving"};

    private final Scanner input;
    private final SaveManager saves;
    private PlayerProgress progress;
    private TaskBoard tasks;
    private int activeSlot;
    private String savedText;

    private TheBaldingGate(Scanner input, SaveManager saves) {
        this.input = input;
        this.saves = saves;
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            new TheBaldingGate(input, new SaveManager(Path.of("saves"))).run();
        }
    }

    private void run() {
        if (!chooseCharacter()) return;
        TaskMenu taskMenu = new TaskMenu(tasks, progress);
        GearShop gearShop = new GearShop(progress);

        while (true) {
            System.out.println("\nCurrency: " + progress.getCurrency() + " | Inventory: "
                    + progress.getInventorySize() + "/" + PlayerProgress.MAX_INVENTORY);
            printOptions(MENU_OPTIONS);
            int action = readChoice(input, 0, MENU_OPTIONS.length);
            if (action == 0) {
                if (confirmExit()) return;
            } else if (action == 1) {
                taskMenu.showTasks();
            } else if (action == 2) {
                int attemptsBefore = tasks.getAttemptsUsed();
                taskMenu.chooseTask(input);
                // Saved characters keep every roll, so reloading cannot undo a failed task.
                if (activeSlot != 0 && tasks.getAttemptsUsed() != attemptsBefore) writeSave(activeSlot);
            } else if (action == 3 && !tasks.isFinished()) {
                System.out.println("You must attempt " + tasks.getAttemptsRemaining() + " more task(s) before buying gear.");
            } else if (action == 3) {
                gearShop.open(input);
            } else if (action == 4) {
                showCharacter();
            } else {
                saveCharacter();
            }
        }
    }

    private boolean chooseCharacter() {
        while (true) {
            System.out.println("THE BALDING GATE");
            printOptions(START_OPTIONS);
            int choice = readChoice(input, 0, START_OPTIONS.length);
            if (choice == 0) return false;
            if (choice == 1) {
                createCharacter();
                return true;
            }
            if (loadCharacter()) return true;
        }
    }

    private void createCharacter() {
        System.out.println("Choose your class:");
        for (int i = 0; i < CLASSES.size(); i++) {
            System.out.printf("%d. %s - %s%n", i + 1, CLASSES.get(i).getName(), CLASSES.get(i).getDescription());
        }
        int choice = readChoice(input, 1, CLASSES.size());
        CombatClass playerClass = CLASSES.get(choice - 1);
        progress = new PlayerProgress(playerClass);
        tasks = new TaskBoard();
        System.out.println("Selected " + playerClass.getName() + ". Starting currency: " + progress.getCurrency());
        System.out.println("Attempt " + TaskBoard.REQUIRED_ATTEMPTS + " of the " + tasks.getTasks().size()
                + " town tasks. Earn currency for gear before battle. A natural 20 is a critical win.");
    }

    private boolean loadCharacter() {
        printSlots();
        System.out.println("0. Back");
        int slot = readChoice(input, 0, SaveManager.SLOT_COUNT);
        if (slot == 0) return false;
        if (!saves.exists(slot)) {
            System.out.println("Slot " + slot + " is empty.");
            return false;
        }
        try {
            SaveManager.SavedGame game = saves.load(slot);
            progress = game.player();
            tasks = game.tasks();
            activeSlot = slot;
            savedText = SaveManager.toText(progress, tasks);
            System.out.println("Loaded " + progress.getCombatClass().getName() + " from slot " + slot + ".");
            return true;
        } catch (IOException e) {
            System.out.println("Could not load slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    private boolean saveCharacter() {
        printSlots();
        System.out.println("0. Back");
        int slot = readChoice(input, 0, SaveManager.SLOT_COUNT);
        if (slot == 0) return false;
        if (slot != activeSlot && saves.exists(slot)) {
            System.out.println("Overwrite slot " + slot + " (" + saves.describe(slot) + ")?");
            System.out.println("1. Yes  0. No");
            if (readChoice(input, 0, 1) == 0) return false;
        }
        return writeSave(slot);
    }

    private boolean writeSave(int slot) {
        try {
            saves.save(slot, progress, tasks);
            activeSlot = slot;
            savedText = SaveManager.toText(progress, tasks);
            System.out.println("Saved to slot " + slot + ".");
            return true;
        } catch (IOException e) {
            System.out.println("Could not save to slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    private boolean confirmExit() {
        if (SaveManager.toText(progress, tasks).equals(savedText)) return true;
        System.out.println("You have unsaved progress.");
        printOptions(EXIT_OPTIONS, "Keep playing");
        int choice = readChoice(input, 0, EXIT_OPTIONS.length);
        if (choice == 0) return false;
        if (choice == 2) return true;
        return activeSlot != 0 ? writeSave(activeSlot) : saveCharacter();
    }

    private void showCharacter() {
        System.out.println(progress.getCombatClass().getName() + " | HP " + progress.getCurrentHealth() + "/" + progress.getMaxHealth()
                + " | Attack " + progress.getAttack() + " | Defense " + progress.getDefense());
        String weapons = gearName(progress.getWeapon())
                + (progress.getOffHandWeapon() == null ? "" : " + " + progress.getOffHandWeapon().getName());
        System.out.println("Weapons: " + weapons + " | Armor: " + gearName(progress.getArmor())
                + " | Class gear: " + gearName(progress.getClassGear()) + " | Potions: " + progress.getPotions().size());
        System.out.println("Inventory: " + progress.getInventorySize() + "/" + PlayerProgress.MAX_INVENTORY);
    }

    private void printSlots() {
        for (int slot = 1; slot <= SaveManager.SLOT_COUNT; slot++) {
            System.out.printf("%d. Slot %d: %s%s%n", slot, slot, saves.describe(slot), slot == activeSlot ? " [CURRENT]" : "");
        }
    }

    private static void printOptions(String[] options) {
        printOptions(options, "Exit");
    }

    private static void printOptions(String[] options, String zeroLabel) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("%d. %s  ", i + 1, options[i]);
        }
        System.out.println("0. " + zeroLabel);
    }

    private static String gearName(Gear item) {
        return item == null ? "None" : item.getName();
    }

    static int readChoice(Scanner input, int min, int max) {
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
