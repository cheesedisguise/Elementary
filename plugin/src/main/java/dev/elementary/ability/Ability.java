package dev.elementary.ability;

import org.bukkit.entity.Player;

public interface Ability {
    String name();
    double cooldownSeconds(int tier);
    /** True if the cast happened (starts the cooldown). */
    boolean cast(Player caster, int tier);
    default boolean ultimate() { return false; }
}
