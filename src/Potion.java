/** Consumable that restores health when used. */
public class Potion extends Gear {
    private final int healing;

    public Potion(String name, int price, int healing) {
        super(name, price);
        if (healing <= 0) throw new IllegalArgumentException("healing must be positive");
        this.healing = healing;
    }

    public int getHealing() { return healing; }

    /** Restores health through the player's progress object; returns actual health restored. */
    public int use(PlayerProgress player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        return player.heal(healing);
    }
}
