package dev.elementary.ability.lightning;

import dev.elementary.ability.Ability;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Overcharge: a six-second stance. While it lasts, EVERY melee hit
 * discharges Static (not every 4th) and each hit arcs to nearby
 * enemies - the actual jolts live in CombatListener, which asks
 * {@link #active} on each swing. The crackling aura is the tell.
 */
public class Overcharge implements Ability {
    private static final Map<UUID, Integer> activeUntil = new HashMap<>();

    private final JavaPlugin plugin;

    public Overcharge(JavaPlugin plugin) { this.plugin = plugin; }

    public static boolean active(Player player) {
        Integer until = activeUntil.get(player.getUniqueId());
        return until != null && Bukkit.getCurrentTick() < until;
    }

    @Override public String name() { return "Overcharge"; }
    @Override public double cooldownSeconds(int tier) { return 35; }

    @Override
    public boolean cast(Player caster, int tier) {
        int end = Bukkit.getCurrentTick() + 20 * 6;
        activeUntil.put(caster.getUniqueId(), end);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.7f);
        new BukkitRunnable() {
            @Override public void run() {
                if (Bukkit.getCurrentTick() >= end || !caster.isOnline()) {
                    activeUntil.remove(caster.getUniqueId(), end);
                    cancel();
                    return;
                }
                double spin = Bukkit.getCurrentTick() * 0.55;
                for (int arm = 0; arm < 2; arm++) {
                    double angle = spin + arm * Math.PI;
                    caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            caster.getLocation().add(0.9 * Math.cos(angle),
                                    1.0 + 0.4 * Math.sin(spin * 0.7),
                                    0.9 * Math.sin(angle)), 2, 0.05, 0.1, 0.05, 0.02);
                }
                if (Bukkit.getCurrentTick() % 30 == 0) {
                    caster.getWorld().playSound(caster.getLocation(),
                            Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.8f);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
