/** Gear that increases the player's attack stat while equipped. */
public class Weapon extends Gear {
    private final int attackBonus;

    public Weapon(String name, int price, int attackBonus) {
        super(name, price);
        if (attackBonus < 0) throw new IllegalArgumentException("attackBonus cannot be negative");
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() { return attackBonus; }
}
