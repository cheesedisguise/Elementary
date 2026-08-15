package dev.elementary.ability.fire;

import dev.elementary.ability.Ability;
import dev.elementary.util.GroundCover;
import dev.elementary.util.Targets;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** A fixed ring of flame; the ground scorches outward, then the border
 *  burns back in and restores itself. */
public class Pyre implements Ability {
    private final JavaPlugin plugin;

    public Pyre(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Pyre"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = tier >= 2 ? 8 : 5;
        GroundCover scorch = GroundCover.spread(plugin, center, radius, 1.4, rng -> {
            int roll = rng.nextInt(100);
            return roll < 60 ? Material.NETHERRACK
                    : roll < 85 ? Material.BASALT : Material.MAGMA_BLOCK;
        });
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 2;
                if (ticks > 20 * 10) {
                    // the scorched border shrinks away to nothing
                    scorch.recede(0.8);
                    cancel();
                    return;
                }
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    Location at = center.clone().add(radius * Math.cos(angle), 0.2,
                            radius * Math.sin(angle));
                    center.getWorld().spawnParticle(Particle.FLAME, at, 2, 0.1, 0.25, 0.1, 0.01);
                    center.getWorld().spawnParticle(Particle.SMALL_FLAME, at, 2,
                            0.25, 0.15, 0.25, 0.01);
                }
                if (ticks % 20 == 0) {
                    ThreadLocalRandom rng = ThreadLocalRandom.current();
                    for (int i = 0; i < 6; i++) {
                        double angle = rng.nextDouble(2 * Math.PI);
                        double dist = rng.nextDouble(radius);
                        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                                center.clone().add(dist * Math.cos(angle), 1.1,
                                        dist * Math.sin(angle)), 1, 0.05, 0.05, 0.05, 0.01);
                    }
                    for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                        boolean inside = target.getLocation().distance(center) <= radius;
                        if (!inside) continue;
                        if (target.equals(caster)) {
                            // territory, not a duel steroid: quickness
                            // to hold the ground, no Strength
                            caster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SPEED, 30, 0, true, false));
                            if (tier >= 2) {
                                caster.addPotionEffect(new PotionEffect(
                                        PotionEffectType.REGENERATION, 30, 0, true, false));
                            }
                        } else if (Targets.hostile(caster, target)) {
                            target.damage(1, caster);
                            target.setFireTicks(Math.max(target.getFireTicks(), 40));
                            center.getWorld().spawnParticle(Particle.LAVA,
                                    target.getLocation(), 2, 0.2, 0.1, 0.2, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
