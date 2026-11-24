/** Immutable starting profile for a playable combat class. */
public abstract class CombatClass {
    private final String name;
    private final String description;
    private final int health;
    private final int attack;
    private final int defense;

    protected CombatClass(String name, String description, int health, int attack, int defense) {
        this.name = name;
        this.description = description;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getHealth() { return health; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
}
