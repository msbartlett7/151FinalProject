import java.util.HashMap;
import java.util.Map;

/** The game's 50 purchasable items, also used to look up gear when loading a save. */
public final class GearCatalog {
    static final Weapon[] MELEE_WEAPONS = {
            new Weapon("Rusty Dagger", 120, 1, true),
            new Weapon("Iron Shortsword", 280, 2, true),
            new Weapon("Hand Axe", 430, 3, true),
            new Weapon("Steel Rapier", 620, 4, true),
            new Weapon("Cavalry Saber", 850, 5, true),
            new Weapon("Hunting Spear", 400, 3),
            new Weapon("Steel Longsword", 700, 5),
            new Weapon("Heavy War Axe", 1100, 7),
            new Weapon("Iron Warhammer", 1450, 9),
            new Weapon("Gatebreaker Greatblade", 1800, 10)
    };

    static final Weapon[] FIREARMS = {
            new Weapon("Flintlock Pistol", 320, 2, true),
            new Weapon("Duelist Revolver", 640, 4, true),
            new Weapon("Repeating Pistol", 950, 6, true),
            new Weapon("Blunderbuss", 520, 4),
            new Weapon("Hunting Rifle", 760, 5),
            new Weapon("Scattergun", 1150, 7),
            new Weapon("Longshot Rifle", 2000, 11)
    };

    static final Weapon[] CLASS_WEAPONS = {
            new Weapon("Veteran's Broadsword", 520, 4, false, "Warrior"),
            new Weapon("Titan Cleaver", 1500, 11, false, "Warrior"),
            new Weapon("Ember Wand", 450, 4, true, "Mage"),
            new Weapon("Archmage Staff", 1550, 12, false, "Mage"),
            new Weapon("Rimefrost Pistol", 500, 4, true, "Frostmancer"),
            new Weapon("Glacier Carbine", 1450, 10, false, "Frostmancer"),
            new Weapon("Yew Shortbow", 470, 4, false, "Ranger"),
            new Weapon("Stormstring Longbow", 1400, 10, false, "Ranger"),
            new Weapon("Consecrated Mace", 500, 4, true, "Paladin"),
            new Weapon("Sunforged Warhammer", 1500, 10, false, "Paladin")
    };

    static final Armor[] ARMOR = {
            new Armor("Padded Tunic", 100, 1, 5),
            new Armor("Leather Vest", 250, 2, 10),
            new Armor("Studded Jerkin", 380, 3, 12),
            new Armor("Chain Shirt", 500, 4, 15),
            new Armor("Brigandine", 650, 5, 18),
            new Armor("Scale Hauberk", 800, 6, 20),
            new Armor("Banded Mail", 1050, 7, 25),
            new Armor("Plate Cuirass", 1300, 8, 30),
            new Armor("Full Plate", 1650, 10, 38),
            new Armor("Gatewarden Plate", 2000, 11, 45)
    };

    static final ClassGear[] CLASS_GEAR = {
            new ClassGear("Ironclad Gauntlets", 400, "Warrior", 2, 1, 5),
            new ClassGear("Warlord's Helm", 1200, "Warrior", 4, 3, 15),
            new ClassGear("Apprentice Focus", 400, "Mage", 3, 0, 0),
            new ClassGear("Archmage Orb", 1200, "Mage", 7, 1, 5),
            new ClassGear("Frostweave Gloves", 400, "Frostmancer", 3, 1, 0),
            new ClassGear("Glacier Mantle", 1200, "Frostmancer", 6, 2, 5),
            new ClassGear("Fletcher's Quiver", 400, "Ranger", 2, 1, 3),
            new ClassGear("Trailwalker Cloak", 1200, "Ranger", 5, 2, 8),
            new ClassGear("Emberblessed Shield", 400, "Paladin", 1, 2, 8),
            new ClassGear("Sunfire Aegis", 1200, "Paladin", 3, 5, 20)
    };

    static final Potion[] POTIONS = {
            new Potion("Minor Healing Potion", 50, 20),
            new Potion("Healing Potion", 120, 45),
            new Potion("Greater Healing Potion", 260, 90)
    };

    private static final Map<String, Gear> BY_NAME = new HashMap<>();

    static {
        register(MELEE_WEAPONS);
        register(FIREARMS);
        register(CLASS_WEAPONS);
        register(ARMOR);
        register(CLASS_GEAR);
        register(POTIONS);
    }

    private GearCatalog() { }

    private static void register(Gear[] items) {
        for (Gear item : items) {
            if (BY_NAME.putIfAbsent(item.getName(), item) != null) {
                throw new IllegalStateException("Duplicate gear name: " + item.getName());
            }
        }
    }

    public static int size() { return BY_NAME.size(); }

    /** Finds an item by exact name and type, throwing when it does not exist. */
    public static <T extends Gear> T find(String name, Class<T> type) {
        Gear item = BY_NAME.get(name);
        if (!type.isInstance(item)) throw new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + name);
        return type.cast(item);
    }
}
