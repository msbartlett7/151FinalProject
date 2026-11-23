/** Base type for purchasable or equipable combat gear. */
public abstract class Gear {
    private final String name;
    private final int price;

    protected Gear(String name, int price) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        if (price < 0) throw new IllegalArgumentException("price cannot be negative");
        this.name = name;
        this.price = price;
    }

    public String getName() { return name; }
    public int getPrice() { return price; }
}
