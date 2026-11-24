import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Currency wallet, five-slot inventory, equipped gear, and health for one player. */
public class PlayerProgress {
    public static final int MAX_INVENTORY = 5;

    public enum PurchaseResult { PURCHASED, NOT_ENOUGH_CURRENCY, INVENTORY_FULL, ALREADY_EQUIPPED, CANNOT_USE }
    public enum Hand { MAIN, OFF }

    private final CombatClass combatClass;
    private int currency;
    private int currentHealth;
    private Weapon weapon;
    private Weapon offHandWeapon;
    private Armor armor;
    private ClassGear classGear;
    private final List<Potion> potions = new ArrayList<>();

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

    public CombatClass getCombatClass() { return combatClass; }
    public int getCurrency() { return currency; }
    public int getMaxHealth() { return combatClass.getHealth() + (armor == null ? 0 : armor.getHealthBonus())
            + (classGear == null ? 0 : classGear.getHealthBonus()); }
    public int getAttack() { return combatClass.getAttack() + (weapon == null ? 0 : weapon.getAttackBonus())
            + (offHandWeapon == null ? 0 : offHandWeapon.getAttackBonus())
            + (classGear == null ? 0 : classGear.getAttackBonus()); }
    public int getDefense() { return combatClass.getDefense() + (armor == null ? 0 : armor.getDefenseBonus())
            + (classGear == null ? 0 : classGear.getDefenseBonus()); }
    public int getCurrentHealth() { return currentHealth; }
    public Weapon getWeapon() { return weapon; }
    public Weapon getOffHandWeapon() { return offHandWeapon; }
    public Armor getArmor() { return armor; }
    public ClassGear getClassGear() { return classGear; }
    public List<Potion> getPotions() { return Collections.unmodifiableList(potions); }

    /** Everything the character carries, with each item using one of the five slots. */
    public List<Gear> getInventory() {
        List<Gear> items = new ArrayList<>();
        if (weapon != null) items.add(weapon);
        if (offHandWeapon != null) items.add(offHandWeapon);
        if (armor != null) items.add(armor);
        if (classGear != null) items.add(classGear);
        items.addAll(potions);
        return items;
    }

    public int getInventorySize() { return getInventory().size(); }

    public PurchaseResult purchaseWeapon(Weapon item) { return purchaseWeapon(item, Hand.MAIN); }

    /**
     * Buys a weapon for one hand and refunds whatever it replaces. A weapon that cannot be carried
     * as a pair takes both hands, so it replaces both weapons currently held.
     */
    public PurchaseResult purchaseWeapon(Weapon item, Hand hand) {
        requireItem(item);
        if (hand == null) throw new IllegalArgumentException("hand cannot be null");
        if (!item.canEquip(combatClass)) return PurchaseResult.CANNOT_USE;
        if (hand == Hand.OFF && (!item.canCarryTwo() || weapon == null || !weapon.canCarryTwo())) {
            return PurchaseResult.CANNOT_USE;
        }
        if (item == (hand == Hand.MAIN ? weapon : offHandWeapon)) return PurchaseResult.ALREADY_EQUIPPED;

        Weapon replacedMain = hand == Hand.MAIN ? weapon : null;
        Weapon replacedOff = hand == Hand.OFF || !item.canCarryTwo() ? offHandWeapon : null;
        int replacedCount = (replacedMain == null ? 0 : 1) + (replacedOff == null ? 0 : 1);
        PurchaseResult result = settle(item, priceOf(replacedMain) + priceOf(replacedOff), replacedCount);
        if (result != PurchaseResult.PURCHASED) return result;

        if (hand == Hand.OFF) {
            offHandWeapon = item;
        } else {
            weapon = item;
            if (!item.canCarryTwo()) offHandWeapon = null;
        }
        return result;
    }

    /** Buys armor and refunds the price of any armor it replaces. */
    public PurchaseResult purchaseArmor(Armor item) {
        requireItem(item);
        if (item == armor) return PurchaseResult.ALREADY_EQUIPPED;
        PurchaseResult result = settle(item, priceOf(armor), armor == null ? 0 : 1);
        if (result != PurchaseResult.PURCHASED) return result;
        int previousMaximum = getMaxHealth();
        armor = item;
        adjustHealth(previousMaximum);
        return result;
    }

    /** Buys class gear and refunds the price of any class gear it replaces. */
    public PurchaseResult purchaseClassGear(ClassGear item) {
        requireItem(item);
        if (!item.canEquip(combatClass)) return PurchaseResult.CANNOT_USE;
        if (item == classGear) return PurchaseResult.ALREADY_EQUIPPED;
        PurchaseResult result = settle(item, priceOf(classGear), classGear == null ? 0 : 1);
        if (result != PurchaseResult.PURCHASED) return result;
        int previousMaximum = getMaxHealth();
        classGear = item;
        adjustHealth(previousMaximum);
        return result;
    }

    public PurchaseResult purchasePotion(Potion item) {
        requireItem(item);
        PurchaseResult result = settle(item, 0, 0);
        if (result == PurchaseResult.PURCHASED) potions.add(item);
        return result;
    }

    /** Removes an inventory item, refunds its full price, and returns the amount refunded. */
    public int returnItem(int inventoryIndex) {
        Gear item = getInventory().get(inventoryIndex);
        int previousMaximum = getMaxHealth();
        if (item instanceof Weapon) {
            if (item != offHandWeapon) weapon = offHandWeapon;
            offHandWeapon = null;
        } else if (item instanceof Armor) {
            armor = null;
        } else if (item instanceof ClassGear) {
            classGear = null;
        } else {
            potions.remove(item);
        }
        adjustHealth(previousMaximum);
        earnCurrency(item.getPrice());
        return item.getPrice();
    }

    public int usePotion(int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex >= potions.size()) throw new IndexOutOfBoundsException("No potion at index " + inventoryIndex);
        Potion item = potions.remove(inventoryIndex);
        return item.use(this);
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

    /** Restores saved health and gear, rejecting any combination the game could not produce. */
    void restoreGear(int health, Weapon savedWeapon, Weapon savedOffHand, Armor savedArmor, ClassGear savedClassGear,
                     List<Potion> savedPotions) {
        if (savedWeapon != null && !savedWeapon.canEquip(combatClass)) throw new IllegalArgumentException("Weapon does not match class");
        if (savedOffHand != null && (savedWeapon == null || !savedWeapon.canCarryTwo()
                || !savedOffHand.canCarryTwo() || !savedOffHand.canEquip(combatClass))) {
            throw new IllegalArgumentException("Invalid second weapon");
        }
        if (savedClassGear != null && !savedClassGear.canEquip(combatClass)) throw new IllegalArgumentException("Class gear does not match class");
        weapon = savedWeapon;
        offHandWeapon = savedOffHand;
        armor = savedArmor;
        classGear = savedClassGear;
        potions.clear();
        potions.addAll(savedPotions);
        if (getInventorySize() > MAX_INVENTORY) throw new IllegalArgumentException("Inventory is over the limit");
        if (health < 0 || health > getMaxHealth()) throw new IllegalArgumentException("Invalid health");
        currentHealth = health;
    }

    private PurchaseResult settle(Gear item, int refund, int replacedCount) {
        if (getInventorySize() - replacedCount + 1 > MAX_INVENTORY) return PurchaseResult.INVENTORY_FULL;
        int cost = item.getPrice() - refund;
        if (currency < cost) return PurchaseResult.NOT_ENOUGH_CURRENCY;
        currency = Math.subtractExact(currency, cost);
        return PurchaseResult.PURCHASED;
    }

    private void adjustHealth(int previousMaximum) {
        currentHealth = Math.min(getMaxHealth(), currentHealth + Math.max(0, getMaxHealth() - previousMaximum));
    }

    private static int priceOf(Gear item) { return item == null ? 0 : item.getPrice(); }

    private static void requireItem(Gear item) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
    }
}
