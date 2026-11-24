/** Gear restricted to a class name; use "*" to allow every class. */
public class ClassGear extends Gear {
    private final String requiredClass;
    private final int attackBonus;
    private final int defenseBonus;
    private final int healthBonus;

    public ClassGear(String name, int price, String requiredClass,
                     int attackBonus, int defenseBonus, int healthBonus) {
        super(name, price);
        if (requiredClass == null || requiredClass.isBlank()) throw new IllegalArgumentException("requiredClass cannot be blank");
        if (attackBonus < 0 || defenseBonus < 0 || healthBonus < 0) throw new IllegalArgumentException("Bonuses cannot be negative");
        this.requiredClass = requiredClass;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.healthBonus = healthBonus;
    }

    public String getRequiredClass() { return requiredClass; }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public int getHealthBonus() { return healthBonus; }

    public boolean canEquip(CombatClass combatClass) {
        return combatClass != null && (requiredClass.equals("*")
                || requiredClass.equalsIgnoreCase(combatClass.getName()));
    }
}
