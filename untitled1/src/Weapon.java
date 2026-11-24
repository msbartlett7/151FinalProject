/** Gear that increases attack. Some weapons can be carried as a pair, and some are class-specific. */
public class Weapon extends Gear {
    public static final String ANY_CLASS = "*";

    private final int attackBonus;
    private final boolean canCarryTwo;
    private final String requiredClass;

    public Weapon(String name, int price, int attackBonus) {
        this(name, price, attackBonus, false, ANY_CLASS);
    }

    public Weapon(String name, int price, int attackBonus, boolean canCarryTwo) {
        this(name, price, attackBonus, canCarryTwo, ANY_CLASS);
    }

    public Weapon(String name, int price, int attackBonus, boolean canCarryTwo, String requiredClass) {
        super(name, price);
        if (attackBonus < 0) throw new IllegalArgumentException("attackBonus cannot be negative");
        if (requiredClass == null || requiredClass.isBlank()) throw new IllegalArgumentException("requiredClass cannot be blank");
        this.attackBonus = attackBonus;
        this.canCarryTwo = canCarryTwo;
        this.requiredClass = requiredClass;
    }

    public int getAttackBonus() { return attackBonus; }
    public boolean canCarryTwo() { return canCarryTwo; }
    public String getRequiredClass() { return requiredClass; }

    public boolean canEquip(CombatClass combatClass) {
        return combatClass != null && (requiredClass.equals(ANY_CLASS)
                || requiredClass.equalsIgnoreCase(combatClass.getName()));
    }
}
