/** Gear that increases defense and optionally maximum health while equipped. */
public class Armor extends Gear {
    private final int defenseBonus;
    private final int healthBonus;

    public Armor(String name, int price, int defenseBonus, int healthBonus) {
        super(name, price);
        if (defenseBonus < 0 || healthBonus < 0) throw new IllegalArgumentException("Bonuses cannot be negative");
        this.defenseBonus = defenseBonus;
        this.healthBonus = healthBonus;
    }

    public int getDefenseBonus() { return defenseBonus; }
    public int getHealthBonus() { return healthBonus; }
}
