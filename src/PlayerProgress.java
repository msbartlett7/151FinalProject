import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Currency wallet and point-buy progression for one player. */
public class PlayerProgress {
    private final CombatClass combatClass;
    private int currency;
    private int unspentPoints;
    private int currentHealth;
    private Weapon weapon;
    private Armor armor;
    private ClassGear classGear;
    private final List<Potion> potions = new ArrayList<>();
    private static final int POINT_COST = 10;

    public PlayerProgress(CombatClass combatClass) { this(combatClass, 0); }

    public PlayerProgress(CombatClass combatClass, int startingCurrency) {
        if (combatClass == null) throw new IllegalArgumentException("combatClass cannot be null");
        if (startingCurrency < 0) throw new IllegalArgumentException("startingCurrency cannot be negative");
        this.combatClass = combatClass;
        this.currency = startingCurrency;
        this.currentHealth = combatClass.getHealth();
    }

    public void earnCurrency(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        currency = Math.addExact(currency, amount);
    }

    public boolean buyPoints(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int cost = Math.multiplyExact(amount, POINT_COST);
        if (currency < cost) return false;
        currency -= cost;
        unspentPoints = Math.addExact(unspentPoints, amount);
        return true;
    }

    public CombatClass getCombatClass() { return combatClass; }
    public int getCurrency() { return currency; }
    public int getUnspentPoints() { return unspentPoints; }
    public int getPointCost() { return POINT_COST; }
    public int getMaxHealth() { return combatClass.getHealth() + (armor == null ? 0 : armor.getHealthBonus())
            + (classGear == null ? 0 : classGear.getHealthBonus()); }
    public int getAttack() { return combatClass.getAttack() + (weapon == null ? 0 : weapon.getAttackBonus())
            + (classGear == null ? 0 : classGear.getAttackBonus()); }
    public int getDefense() { return combatClass.getDefense() + (armor == null ? 0 : armor.getDefenseBonus())
            + (classGear == null ? 0 : classGear.getDefenseBonus()); }
    public int getCurrentHealth() { return currentHealth; }
    public Weapon getWeapon() { return weapon; }
    public Armor getArmor() { return armor; }
    public ClassGear getClassGear() { return classGear; }
    public List<Potion> getPotions() { return Collections.unmodifiableList(potions); }

    public void equipWeapon(Weapon weapon) { this.weapon = weapon; }
    public void equipArmor(Armor armor) {
        int previousMaximum = getMaxHealth();
        this.armor = armor;
        currentHealth = Math.min(getMaxHealth(), currentHealth + Math.max(0, getMaxHealth() - previousMaximum));
    }
    public boolean equipClassGear(ClassGear gear) {
        if (gear != null && !gear.canEquip(combatClass)) return false;
        int previousMaximum = getMaxHealth();
        this.classGear = gear;
        currentHealth = Math.min(getMaxHealth(), currentHealth + Math.max(0, getMaxHealth() - previousMaximum));
        return true;
    }

    /** Purchases and equips a weapon, leaving the wallet unchanged when unaffordable. */
    public boolean purchaseWeapon(Weapon item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (!spendCurrency(item.getPrice())) return false;
        equipWeapon(item);
        return true;
    }

    public boolean purchaseArmor(Armor item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (!spendCurrency(item.getPrice())) return false;
        equipArmor(item);
        return true;
    }

    public boolean purchaseClassGear(ClassGear item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (!item.canEquip(combatClass)) return false;
        if (!spendCurrency(item.getPrice())) return false;
        return equipClassGear(item);
    }

    public boolean purchasePotion(Potion item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (!spendCurrency(item.getPrice())) return false;
        potions.add(item);
        return true;
    }

    public int usePotion(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= potions.size()) throw new IndexOutOfBoundsException("No potion at index " + inventoryIndex);
        Potion item = potions.remove(inventoryIndex);
        return item.use(this);
    }

    private boolean spendCurrency(int amount) {
        if (currency < amount) return false;
        currency -= amount;
        return true;
    }

    public int heal(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        int restored = Math.min(amount, getMaxHealth() - currentHealth);
        currentHealth += restored;
        return restored;
    }

    /** Applies incoming damage, clamping health at zero, and returns damage actually taken. */
    public int takeDamage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        int taken = Math.min(amount, currentHealth);
        currentHealth -= taken;
        return taken;
    }
}
