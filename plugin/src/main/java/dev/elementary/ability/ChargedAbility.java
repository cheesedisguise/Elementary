package dev.elementary.ability;

import org.bukkit.entity.Player;

/**
 * An ability cast by holding right-click. The client re-fires interact
 * events every few ticks while the button is held; AbilityManager feeds
 * those repeats here instead of treating them as new casts, and the
 * ability fires itself when the stream stops (= button released).
 */
public interface ChargedAbility extends Ability {
    boolean charging(Player player);

    void feed(Player player);
}
