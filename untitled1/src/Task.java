import java.util.ArrayList;
import java.util.List;

/**
 * An objective that awards currency. Guaranteed tasks always succeed but can only be attempted once.
 * Pass/fail tasks need a d20 roll at or above a target, allow up to three tries, and lock after one
 * success. Tiered tasks always pay, with the d20 roll setting the amount, and can be completed twice.
 */
public class Task {
    public static final int DICE_SIDES = 20;
    public static final int RISKY_MAX_ATTEMPTS = 3;
    public static final int VARIABLE_MAX_SUCCESSES = 2;
    public static final int CRITICAL_MULTIPLIER = 2;

    /** Pays reward for rolls from minRoll up to the next tier's minRoll. */
    public record RewardTier(int minRoll, int reward) { }

    private final int id;
    private final String title;
    private final String description;
    private final int reward;
    private final int targetRoll;
    private final int maxAttempts;
    private final int maxSuccesses;
    private final List<RewardTier> tiers;
    private int attempts;
    private int successes;
    private int lastRoll;
    private int lastReward;

    /** Creates a guaranteed task. */
    public Task(int id, String title, String description, int reward) {
        this(id, title, description, reward, 0, 1, List.of());
    }

    /** Creates a pass/fail task that succeeds when a d20 roll meets targetRoll (2 to 20). */
    public Task(int id, String title, String description, int reward, int targetRoll) {
        this(id, title, description, reward, targetRoll, RISKY_MAX_ATTEMPTS, List.of());
        if (targetRoll < 2 || targetRoll > DICE_SIDES) throw new IllegalArgumentException("Invalid target roll");
    }

    /** Creates a tiered task. Tiers must start at roll 1 and rise in both roll and reward. */
    public Task(int id, String title, String description, RewardTier... tiers) {
        this(id, title, description, highestReward(tiers), 1, VARIABLE_MAX_SUCCESSES, validateTiers(tiers));
    }

    private Task(int id, String title, String description, int reward, int targetRoll, int maxAttempts,
                 List<RewardTier> tiers) {
        if (id < 1 || reward < 0) throw new IllegalArgumentException("Invalid task id or reward");
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.targetRoll = targetRoll;
        this.maxAttempts = maxAttempts;
        this.maxSuccesses = tiers.isEmpty() ? 1 : VARIABLE_MAX_SUCCESSES;
        this.tiers = tiers;
    }

    private static int highestReward(RewardTier[] tiers) {
        List<RewardTier> checked = validateTiers(tiers);
        return checked.get(checked.size() - 1).reward();
    }

    private static List<RewardTier> validateTiers(RewardTier[] tiers) {
        if (tiers == null || tiers.length == 0 || tiers[0].minRoll() != 1) {
            throw new IllegalArgumentException("Tiers must start at roll 1");
        }
        List<RewardTier> checked = new ArrayList<>();
        for (RewardTier tier : tiers) {
            if (tier.minRoll() > DICE_SIDES || tier.reward() <= 0) throw new IllegalArgumentException("Invalid tier");
            if (!checked.isEmpty()) {
                RewardTier previous = checked.get(checked.size() - 1);
                if (tier.minRoll() <= previous.minRoll() || tier.reward() <= previous.reward()) {
                    throw new IllegalArgumentException("Tiers must rise in roll and reward");
                }
            }
            checked.add(tier);
        }
        return List.copyOf(checked);
    }

    public int getId() { return id; }
    public String getTitle() { return title; }

    /** The normal success reward, or the top tier's reward for a tiered task. */
    public int getReward() { return reward; }
    public int getTargetRoll() { return targetRoll; }
    public int getLastRoll() { return lastRoll; }
    public boolean isLastRollCritical() { return lastRoll == DICE_SIDES; }
    public int getLastReward() { return lastReward; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getSuccesses() { return successes; }
    public int getMaxSuccesses() { return maxSuccesses; }
    public List<RewardTier> getTiers() { return tiers; }
    public boolean isTiered() { return !tiers.isEmpty(); }
    public boolean isGuaranteed() { return targetRoll == 0; }
    public boolean canAttempt() { return attempts < maxAttempts && successes < maxSuccesses; }

    /** True once this task has no attempts left. */
    public boolean isCompleted() { return !canAttempt(); }

    /** Includes the task's odds and attempts so any task list shows them. */
    public String getDescription() {
        if (isGuaranteed()) return description + " [Guaranteed]";
        if (isTiered()) {
            return description + " [Tiered d20: " + describeTiers() + ", completed " + successes + "/" + maxSuccesses + "]";
        }
        return description + " [Risky: roll " + targetRoll + "+ on d20, crit 20 pays "
                + reward * CRITICAL_MULTIPLIER + ", attempts " + attempts + "/" + maxAttempts + ", pays once]";
    }

    private String describeTiers() {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < tiers.size(); i++) {
            int low = tiers.get(i).minRoll();
            int high = i + 1 < tiers.size() ? tiers.get(i + 1).minRoll() - 1 : DICE_SIDES;
            String rolls = low == DICE_SIDES ? "crit 20" : low == high ? String.valueOf(low) : low + "-" + high;
            parts.add(rolls + " pays " + tiers.get(i).reward());
        }
        return String.join(", ", parts);
    }

    /** Returns a whole number from 1 to 20. */
    static int rollD20() {
        return (int) (Math.random() * DICE_SIDES) + 1;
    }

    /** Returns what this task pays for a roll. A natural 20 doubles a pass/fail reward. */
    int rewardForRoll(int roll) {
        if (isTiered()) {
            int payout = 0;
            for (RewardTier tier : tiers) {
                if (roll >= tier.minRoll()) payout = tier.reward();
            }
            return payout;
        }
        if (roll == DICE_SIDES) return reward * CRITICAL_MULTIPLIER;
        return roll >= targetRoll ? reward : 0;
    }

    /** Restores saved progress, rejecting counts this task could not reach. */
    void restore(int savedAttempts, int savedSuccesses) {
        boolean inRange = savedAttempts >= 0 && savedAttempts <= maxAttempts
                && savedSuccesses >= 0 && savedSuccesses <= maxSuccesses && savedSuccesses <= savedAttempts;
        boolean alwaysPays = isGuaranteed() || isTiered();
        if (!inRange || (alwaysPays && savedSuccesses != savedAttempts)) {
            throw new IllegalArgumentException("Invalid progress for task " + id);
        }
        attempts = savedAttempts;
        successes = savedSuccesses;
    }

    /** Uses one attempt and returns whether it paid out. */
    boolean attempt() {
        if (!canAttempt()) throw new IllegalStateException("No attempts left for task " + id);
        attempts++;
        if (isGuaranteed()) {
            lastReward = reward;
        } else {
            lastRoll = rollD20();
            lastReward = rewardForRoll(lastRoll);
        }
        boolean succeeded = lastReward > 0;
        if (succeeded) successes++;
        return succeeded;
    }
}
