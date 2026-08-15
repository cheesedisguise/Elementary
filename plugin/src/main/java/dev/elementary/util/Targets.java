package dev.elementary.util;

import dev.elementary.trust.TrustService;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** One place that answers "may this ability affect that entity?". */
public final class Targets {
    private static TrustService trust;

    private Targets() {}

    public static void init(TrustService service) { trust = service; }

    /** True if a harmful effect from this caster may touch the target. */
    public static boolean hostile(Player caster, Entity target) {
        if (target.equals(caster) || !(target instanceof LivingEntity)) return false;
        if (target instanceof Player other) {
            if (other.getGameMode() == GameMode.SPECTATOR) return false;
            if (trust != null && trust.trusts(caster.getUniqueId(), other.getUniqueId())) {
                return false;
            }
        }
        return true;
    }

    /** True if a supportive effect from this caster should include them. */
    public static boolean friendly(Player caster, Player other) {
        return other.equals(caster)
                || (trust != null && trust.trusts(caster.getUniqueId(), other.getUniqueId()));
    }
}
