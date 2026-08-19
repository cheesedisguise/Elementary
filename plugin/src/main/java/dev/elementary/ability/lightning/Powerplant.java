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
 * Powerplant: thirty seconds as a live generator. The Momentum cap
 * rises from 15 to 20 blocks/sec and charge builds 50% faster, every
 * Lightning move hits 25% harder, Over-Charged zaps jump to 2 bonus
 * damage, and a crackling halo spins over the caster's head. The
 * cooldown only starts counting once the plant winds down (90s on the
 * clock = 60s after the 30s uptime).
 */
public class Powerplant implements Ability {
    private static final int DURATION = 30 * 20;

    private static final Map<UUID, Integer> activeUntil = new HashMap<>();

    private final JavaPlugin plugin;

    public Powerplant(JavaPlugin plugin) { this.plugin = plugin; }

    public static boolean active(Player player) {
        Integer until = activeUntil.get(player.getUniqueId());
        return until != null && Bukkit.getCurrentTick() < until;
    }

    /** +25% on every Lightning move while the plant runs. */
    public static double moveMultiplier(Player player) {
        return active(player) ? 1.25 : 1.0;
    }

    @Override public String name() { return "Powerplant"; }
    @Override public double cooldownSeconds(int tier) { return 90; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        int end = Bukkit.getCurrentTick() + DURATION;
        activeUntil.put(caster.getUniqueId(), end);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.9f);
        new BukkitRunnable() {
            @Override public void run() {
                if (Bukkit.getCurrentTick() >= end || !caster.isOnline()
                        || caster.isDead()) {
                    activeUntil.remove(caster.getUniqueId(), end);
                    if (caster.isOnline()) {
                        caster.getWorld().playSound(caster.getLocation(),
                                Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.4f);
                    }
                    cancel();
                    return;
                }
                // the halo: a ring of charge spinning over the head
                double spin = Bukkit.getCurrentTick() * 0.5;
                for (int arm = 0; arm < 3; arm++) {
                    double angle = spin + arm * 2 * Math.PI / 3;
                    caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            caster.getLocation().add(0.55 * Math.cos(angle), 2.45,
                                    0.55 * Math.sin(angle)), 1, 0.03, 0.03, 0.03, 0.01);
                }
                if (Bukkit.getCurrentTick() % 40 == 0) {
                    caster.getWorld().playSound(caster.getLocation(),
                            Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.9f);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
