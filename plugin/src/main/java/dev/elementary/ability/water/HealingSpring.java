package dev.elementary.ability.water;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Healing Spring: a pool of renewal fixed at the cast spot for 8s.
 * The caster and everyone they /trust standing in it are healed,
 * extinguished and cleansed. Enemies get nothing but wet feet.
 */
public class HealingSpring implements Ability {
    private final JavaPlugin plugin;

    public HealingSpring(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Healing Spring"; }
    @Override public double cooldownSeconds(int tier) { return 35; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = tier >= 2 ? 6 : 4;
        center.getWorld().playSound(center, Sound.ENTITY_DOLPHIN_SPLASH, 1.2f, 1.1f);
        center.getWorld().playSound(center, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,
                1.4f, 0.8f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 5;
                if (ticks > 20 * 8) { cancel(); return; }
                for (int i = 0; i < 18; i++) {
                    double angle = 2 * Math.PI * i / 18;
                    center.getWorld().spawnParticle(Particle.SPLASH,
                            center.clone().add(radius * Math.cos(angle), 0.25,
                                    radius * Math.sin(angle)), 2, 0.1, 0.05, 0.1, 0);
                }
                center.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                        center.clone().add(0, 1.6, 0), 4, radius * 0.5, 0.8, radius * 0.5, 0);
                center.getWorld().spawnParticle(Particle.BUBBLE_POP,
                        center.clone().add(0, 0.4, 0), 3, radius * 0.4, 0.2, radius * 0.4, 0.01);
                if (ticks % 20 != 0) return;
                for (Player bather : center.getNearbyPlayers(radius)) {
                    if (!Targets.friendly(caster, bather)) continue;
                    bather.addPotionEffect(new PotionEffect(
                            PotionEffectType.REGENERATION, 45, 1, true, false));
                    if (tier >= 2) {
                        bather.addPotionEffect(new PotionEffect(
                                PotionEffectType.ABSORPTION, 60, 0, true, false));
                    }
                    bather.setFireTicks(0);
                    bather.removePotionEffect(PotionEffectType.POISON);
                    bather.removePotionEffect(PotionEffectType.WITHER);
                    bather.removePotionEffect(PotionEffectType.BLINDNESS);
                    bather.removePotionEffect(PotionEffectType.NAUSEA);
                    bather.getWorld().spawnParticle(Particle.HEART,
                            bather.getLocation().add(0, 2, 0), 1, 0.25, 0.15, 0.25);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
        return true;
    }
}
