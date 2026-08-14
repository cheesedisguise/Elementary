package dev.elementary.element;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Element {
    EARTH("Earth", NamedTextColor.GREEN, 1001, 1002, false),
    WATER("Water", NamedTextColor.AQUA, 1011, 1012, false),
    FIRE("Fire", NamedTextColor.RED, 1021, 1022, false),
    AIR("Air", NamedTextColor.WHITE, 1031, 1032, false),
    /** In the pool for everyone; aqudr is bound to it and always spawns with it. */
    ICE("Ice", TextColor.color(0x6FD9F2), 1041, 1042, false),
    SHADOW("Shadow", TextColor.color(0xC0404E), 1051, 1052, false),
    LIGHT("Light", TextColor.color(0xFFF3A8), 1061, 1062, false),
    LIGHTNING("Lightning", TextColor.color(0xFFE21F), 1071, 1072, false);

    private final String displayName;
    private final TextColor color;
    private final int cmdTier1;
    private final int cmdTier2;
    private final boolean bound;

    Element(String displayName, TextColor color, int cmdTier1, int cmdTier2, boolean bound) {
        this.displayName = displayName;
        this.color = color;
        this.cmdTier1 = cmdTier1;
        this.cmdTier2 = cmdTier2;
        this.bound = bound;
    }

    public String displayName() { return displayName; }
    public TextColor color() { return color; }
    public int cmd(int tier) { return tier >= 2 ? cmdTier2 : cmdTier1; }
    /** Bound elements are excluded from rolls and Shard Traders. */
    public boolean boundOnly() { return bound; }
    public String shardName() { return displayName + " Shard"; }
}
