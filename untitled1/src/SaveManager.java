import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Reads and writes characters as plain-text files in numbered save slots. */
public class SaveManager {
    public static final int SLOT_COUNT = 3;
    private static final int SAVE_VERSION = 1;
    private static final String POTION_SEPARATOR = "|";

    public record SavedGame(PlayerProgress player, TaskBoard tasks) { }

    private final Path directory;

    public SaveManager(Path directory) {
        if (directory == null) throw new IllegalArgumentException("directory cannot be null");
        this.directory = directory;
    }

    public Path pathFor(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) throw new IllegalArgumentException("Unknown save slot: " + slot);
        return directory.resolve("slot" + slot + ".txt");
    }

    public boolean exists(int slot) { return Files.isRegularFile(pathFor(slot)); }

    /** Writes to a temporary file first so a failed save cannot damage the existing one. */
    public void save(int slot, PlayerProgress player, TaskBoard tasks) throws IOException {
        Path target = pathFor(slot);
        Files.createDirectories(directory);
        Path temp = directory.resolve("slot" + slot + ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(toText(player, tasks));
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Loads a slot, throwing IOException when the file is missing, unreadable, or invalid. */
    public SavedGame load(int slot) throws IOException {
        Map<String, String> values = readValues(pathFor(slot));
        try {
            return parse(values);
        } catch (IllegalArgumentException e) {
            throw new IOException("Save file is damaged: " + e.getMessage(), e);
        }
    }

    /** Returns a one-line summary of a slot for menus. */
    public String describe(int slot) {
        if (!exists(slot)) return "Empty";
        try {
            SavedGame game = load(slot);
            return game.player().getCombatClass().getName() + " | " + game.player().getCurrency() + " currency | Tasks "
                    + game.tasks().getAttemptsUsed() + "/" + TaskBoard.REQUIRED_ATTEMPTS + " | Inventory "
                    + game.player().getInventorySize() + "/" + PlayerProgress.MAX_INVENTORY;
        } catch (IOException e) {
            return "Damaged save";
        }
    }

    /** Builds the exact text stored in a save file. */
    public static String toText(PlayerProgress player, TaskBoard tasks) {
        StringBuilder text = new StringBuilder();
        appendLine(text, "version", String.valueOf(SAVE_VERSION));
        appendLine(text, "class", player.getCombatClass().getName());
        appendLine(text, "currency", String.valueOf(player.getCurrency()));
        appendLine(text, "health", String.valueOf(player.getCurrentHealth()));
        appendLine(text, "weapon", nameOf(player.getWeapon()));
        appendLine(text, "offhand", nameOf(player.getOffHandWeapon()));
        appendLine(text, "armor", nameOf(player.getArmor()));
        appendLine(text, "classgear", nameOf(player.getClassGear()));
        appendLine(text, "potions", player.getPotions().stream().map(Gear::getName)
                .collect(Collectors.joining(POTION_SEPARATOR)));
        for (Task task : tasks.getTasks()) {
            appendLine(text, "task." + task.getId(), task.getAttempts() + "/" + task.getSuccesses());
        }
        return text.toString();
    }

    private static void appendLine(StringBuilder text, String key, String value) {
        text.append(key).append('=').append(value).append('\n');
    }

    private static String nameOf(Gear item) { return item == null ? "" : item.getName(); }

    private static Map<String, String> readValues(Path file) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                int separator = line.indexOf('=');
                if (separator < 1 || values.putIfAbsent(line.substring(0, separator), line.substring(separator + 1)) != null) {
                    throw new IOException("Save file is damaged: bad line");
                }
            }
        }
        return values;
    }

    private static SavedGame parse(Map<String, String> values) {
        if (!String.valueOf(SAVE_VERSION).equals(required(values, "version"))) {
            throw new IllegalArgumentException("unsupported version");
        }
        String className = required(values, "class");
        CombatClass combatClass = TheBaldingGate.CLASSES.stream()
                .filter(candidate -> candidate.getName().equals(className))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown class"));

        PlayerProgress player = new PlayerProgress(combatClass, Integer.parseInt(required(values, "currency")));
        List<Potion> potions = new ArrayList<>();
        String potionNames = required(values, "potions");
        if (!potionNames.isEmpty()) {
            for (String name : potionNames.split("\\" + POTION_SEPARATOR, -1)) {
                potions.add(GearCatalog.find(name, Potion.class));
            }
        }
        player.restoreGear(Integer.parseInt(required(values, "health")),
                optionalGear(values, "weapon", Weapon.class),
                optionalGear(values, "offhand", Weapon.class),
                optionalGear(values, "armor", Armor.class),
                optionalGear(values, "classgear", ClassGear.class),
                potions);

        TaskBoard tasks = new TaskBoard();
        int taskCount = tasks.getTasks().size();
        int[] attempts = new int[taskCount];
        int[] successes = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            String[] parts = required(values, "task." + (i + 1)).split("/", -1);
            if (parts.length != 2) throw new IllegalArgumentException("bad task entry");
            attempts[i] = Integer.parseInt(parts[0]);
            successes[i] = Integer.parseInt(parts[1]);
        }
        tasks.restore(attempts, successes);
        return new SavedGame(player, tasks);
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static <T extends Gear> T optionalGear(Map<String, String> values, String key, Class<T> type) {
        String name = required(values, key);
        return name.isEmpty() ? null : GearCatalog.find(name, type);
    }
}
