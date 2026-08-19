package dev.elementary.util;

import dev.elementary.status.StatusService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Armour-, absorption- and i-frame-bypassing damage. True damage
 *  ignores equipment - never statuses: Fear, Luminosity, Absolute
 *  Radiance, Harmony and Hunt all scale it like any other hit. */
public final class TrueDamage {
    private static StatusService status;

    private TrueDamage() {}

    public static void init(StatusService service) { status = service; }

    public static void apply(LivingEntity target, double amount, Player source) {
        if (target.isDead() || target.isInvulnerable()) return;
        if (target instanceof Player p && p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (status != null) {
            amount *= status.damageInMultiplier(target, source);
            if (source != null) {
                amount *= status.damageOutMultiplier(source);
                amount *= dev.elementary.ability.shadow.Hunt
                        .damageMultiplier(source, target);
            }
        }
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
