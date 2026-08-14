package dev.elementary.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Armour-, absorption- and i-frame-bypassing damage. */
public final class TrueDamage {
    private TrueDamage() {}

    public static void apply(LivingEntity target, double amount, Player source) {
        if (target.isDead() || target.isInvulnerable()) return;
        if (target instanceof Player p && p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        double health = target.getHealth() - amount;
        if (health <= 0) {
            // lethal: route through a real damage call for death handling
            target.setNoDamageTicks(0);
            target.damage(Math.max(amount, target.getHealth()), source);
        } else {
            target.setHealth(health);
            target.playHurtAnimation(0);
        }
    }
}
