import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** Array-based console shop where task currency buys, trades in, and returns gear. */
public class GearShop {
    private static final String[] SECTION_LABELS = {
            "===== MELEE WEAPONS =====",
            "===== FIREARMS =====",
            "===== CLASS WEAPONS =====",
            "===== ARMOR =====",
            "===== CLASS GEAR =====",
            "===== POTIONS ====="
    };

    private final PlayerProgress player;
    private final Gear[][] sections;

    public GearShop(PlayerProgress player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        this.player = player;
        Weapon[] classWeapons = Arrays.stream(GearCatalog.CLASS_WEAPONS)
                .filter(item -> item.canEquip(player.getCombatClass()))
                .toArray(Weapon[]::new);
        ClassGear[] classGear = Arrays.stream(GearCatalog.CLASS_GEAR)
                .filter(item -> item.canEquip(player.getCombatClass()))
                .toArray(ClassGear[]::new);
        sections = new Gear[][] {GearCatalog.MELEE_WEAPONS, GearCatalog.FIREARMS, classWeapons,
                GearCatalog.ARMOR, classGear, GearCatalog.POTIONS};
    }

    /** Shows the labeled catalog until the player leaves the shop. */
    public void open(Scanner input) {
        Gear[] options = Arrays.stream(sections).flatMap(Arrays::stream).toArray(Gear[]::new);
        int returnOption = options.length + 1;
        while (true) {
            System.out.println("\nGEAR SHOP | Currency: " + player.getCurrency() + " | Inventory: "
                    + player.getInventorySize() + "/" + PlayerProgress.MAX_INVENTORY);
            System.out.println("Replacing a weapon, armor, or class gear refunds the old item's price.");
            int number = 0;
            for (int section = 0; section < sections.length; section++) {
                System.out.println("\n" + SECTION_LABELS[section]);
                for (Gear item : sections[section]) {
                    number++;
                    System.out.printf("%2d. %s - %s (%d currency)%s%n", number, item.getName(), describe(item),
                            item.getPrice(), equippedTag(item));
                }
            }
            System.out.printf("%n%2d. Return an item for a full refund%n", returnOption);
            System.out.println(" 0. Leave shop");
            int selection = TheBaldingGate.readChoice(input, 0, returnOption);
            if (selection == 0) return;
            if (selection == returnOption) returnItem(input);
            else buy(input, options[selection - 1]);
        }
    }

    private void buy(Scanner input, Gear item) {
        int currencyBefore = player.getCurrency();
        PlayerProgress.PurchaseResult result;
        if (item instanceof Weapon weapon) {
            PlayerProgress.Hand hand = chooseHand(input, weapon);
            if (hand == null) return;
            result = player.purchaseWeapon(weapon, hand);
        } else if (item instanceof Armor armor) {
            result = player.purchaseArmor(armor);
        } else if (item instanceof ClassGear classGear) {
            result = player.purchaseClassGear(classGear);
        } else {
            result = player.purchasePotion((Potion) item);
        }
        switch (result) {
            case PURCHASED -> reportPurchase(item, currencyBefore);
            case NOT_ENOUGH_CURRENCY -> System.out.println("Not enough currency for " + item.getName() + ".");
            case INVENTORY_FULL -> System.out.println("Your inventory is full ("
                    + PlayerProgress.MAX_INVENTORY + "/" + PlayerProgress.MAX_INVENTORY + "). Return an item first.");
            case ALREADY_EQUIPPED -> System.out.println(item.getName() + " is already equipped.");
            case CANNOT_USE -> System.out.println("You cannot use " + item.getName() + ".");
        }
    }

    /** Asks where a pairable weapon goes, or returns null when the player cancels. */
    private PlayerProgress.Hand chooseHand(Scanner input, Weapon item) {
        Weapon main = player.getWeapon();
        Weapon off = player.getOffHandWeapon();
        if (main == null || !main.canCarryTwo() || !item.canCarryTwo()) return PlayerProgress.Hand.MAIN;

        String[] labels = new String[2];
        PlayerProgress.Hand[] hands = new PlayerProgress.Hand[2];
        int count = 0;
        if (off == null) {
            labels[count] = "Carry as your second weapon";
            hands[count++] = PlayerProgress.Hand.OFF;
        }
        if (main != item) {
            labels[count] = "Replace " + main.getName();
            hands[count++] = PlayerProgress.Hand.MAIN;
        }
        if (off != null && off != item) {
            labels[count] = "Replace " + off.getName();
            hands[count++] = PlayerProgress.Hand.OFF;
        }
        if (count == 0) {
            System.out.println("You are already carrying two of " + item.getName() + ".");
            return null;
        }
        for (int i = 0; i < count; i++) System.out.println((i + 1) + ". " + labels[i]);
        System.out.println("0. Cancel");
        int selection = TheBaldingGate.readChoice(input, 0, count);
        return selection == 0 ? null : hands[selection - 1];
    }

    private void reportPurchase(Gear item, int currencyBefore) {
        int netCost = currencyBefore - player.getCurrency();
        int tradeIn = item.getPrice() - netCost;
        String payment = netCost > 0 ? "Paid " + netCost : netCost < 0 ? "Refunded " + -netCost : "Even trade";
        System.out.println("Bought " + item.getName() + (tradeIn > 0 ? " with a " + tradeIn + " trade-in" : "")
                + ". " + payment + ". Currency: " + player.getCurrency() + ".");
    }

    private void returnItem(Scanner input) {
        Gear[] items = player.getInventory().toArray(new Gear[0]);
        if (items.length == 0) {
            System.out.println("Your inventory is empty.");
            return;
        }
        for (int i = 0; i < items.length; i++) {
            System.out.printf("%d. %s (refund %d)%n", i + 1, items[i].getName(), items[i].getPrice());
        }
        System.out.println("0. Back");
        int selection = TheBaldingGate.readChoice(input, 0, items.length);
        if (selection == 0) return;
        int refund = player.returnItem(selection - 1);
        System.out.println("Returned " + items[selection - 1].getName() + ". Refunded " + refund
                + ". Currency: " + player.getCurrency() + ".");
    }

    private String equippedTag(Gear item) {
        boolean main = item == player.getWeapon();
        boolean off = item == player.getOffHandWeapon();
        if (main && off) return " [EQUIPPED x2]";
        return main || off || item == player.getArmor() || item == player.getClassGear() ? " [EQUIPPED]" : "";
    }

    private static String describe(Gear item) {
        if (item instanceof Weapon weapon) {
            return bonuses(weapon.getAttackBonus(), 0, 0) + (weapon.canCarryTwo() ? ", can carry two" : "");
        }
        if (item instanceof Armor armor) return bonuses(0, armor.getDefenseBonus(), armor.getHealthBonus());
        if (item instanceof ClassGear gear) {
            return bonuses(gear.getAttackBonus(), gear.getDefenseBonus(), gear.getHealthBonus());
        }
        return "restores " + ((Potion) item).getHealing() + " health";
    }

    private static String bonuses(int attack, int defense, int health) {
        List<String> parts = new ArrayList<>();
        if (attack > 0) parts.add("+" + attack + " attack");
        if (defense > 0) parts.add("+" + defense + " defense");
        if (health > 0) parts.add("+" + health + " health");
        return String.join(", ", parts);
    }
}
