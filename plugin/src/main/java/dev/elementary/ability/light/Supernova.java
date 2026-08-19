package dev.elementary.ability.light;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.status.StatusService;
import dev.elementary.util.Targets;
import dev.elementary.util.TrueDamage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Supernova: all five Radiance stacks detonate at once. Everything
 * hostile within eight blocks takes the blast and is painted with
 * Luminosity - and anyone ALREADY luminous is upgraded to Absolute
 * Radiance instead. If the caster is in Harmony when it goes off, the
 * afterglow keeps firing: an automatic beam at the nearest enemy every
 * second for ten seconds.
 */
public class Supernova implements dev.elementary.ability.Ability {
    public static final int COST = 5;

    private final ElementaryPlugin plugin;

    public Supernova(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Supernova"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        if (plugin.radiance().stacks(caster) < COST) {
            Msg.fail(caster, "Needs " + COST + " Radiance");
            return false;
        }
        plugin.radiance().spend(caster, COST);
        boolean harmonic = plugin.status().has(caster, StatusService.Status.HARMONY);
        Location center = caster.getLocation().clone().add(0, 1, 0);

        caster.getWorld().spawnParticle(Particle.FLASH, center, 1);
        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.6f);
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 1.5f);
        caster.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.9f);
        ring(center);

        double radius = 8;
        for (LivingEntity victim : center.getNearbyLivingEntities(radius)) {
            if (!Targets.hostile(caster, victim)) continue;
            boolean undead = Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType());
            victim.damage(undead ? 9 : 6, caster);
            if (plugin.status().has(victim, StatusService.Status.LUMINOSITY)) {
                // already luminous: the light claims them entirely
                plugin.status().apply(victim, StatusService.Status.ABSOLUTE_RADIANCE,
                        10 * 20, caster);
            } else {
                plugin.status().apply(victim, StatusService.Status.LUMINOSITY,
                        5 * 20, caster);
            }
            victim.getWorld().spawnParticle(Particle.END_ROD,
                    victim.getEyeLocation(), 14, 0.3, 0.4, 0.3, 0.09);
        }

        if (harmonic) afterglow(caster);
        return true;
    }

    private void ring(Location center) {
        new BukkitRunnable() {
            double radius = 0.8;
            @Override public void run() {
                if (radius > 8.5) { cancel(); return; }
                for (int i = 0; i < 36; i++) {
                    double a = 2 * Math.PI * i / 36;
                    center.getWorld().spawnParticle(Particle.DUST, center.clone()
                            .add(radius * Math.cos(a), 0, radius * Math.sin(a)), 1,
                            0.05, 0.1, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFF3A8), 1.5f));
                }
                radius += 0.9;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /** Harmony afterglow: one homing beam per second, ten seconds. */
    private void afterglow(Player caster) {
        caster.playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        new BukkitRunnable() {
            int shots = 0;
            @Override public void run() {
                shots++;
                if (shots > 10 || !caster.isOnline() || caster.isDead()) {
                    cancel();
                    return;
                }
                LivingEntity nearest = null;
                double best = 16 * 16;
                for (LivingEntity target : caster.getLocation()
                        .getNearbyLivingEntities(16)) {
                    if (!Targets.hostile(caster, target)) continue;
                    double d = target.getLocation()
                            .distanceSquared(caster.getLocation());
                    if (d < best) { best = d; nearest = target; }
                }
                if (nearest == null) return;
                beam(caster, nearest);
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private void beam(Player caster, LivingEntity victim) {
        org.bukkit.util.Vector from = caster.getEyeLocation().toVector();
        org.bukkit.util.Vector to = victim.getEyeLocation().toVector();
        org.bukkit.util.Vector step = to.clone().subtract(from);
        double length = Math.max(0.01, step.length());
        step.normalize().multiply(0.5);
        org.bukkit.util.Vector cursor = from.clone();
        for (double d = 0; d < length; d += 0.5) {
            cursor.add(step);
            caster.getWorld().spawnParticle(Particle.END_ROD,
                    cursor.toLocation(caster.getWorld()), 1, 0.02, 0.02, 0.02, 0.001);
        }
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_ALLAY_ITEM_THROWN, 0.8f, 1.6f);
        TrueDamage.apply(victim, 2, caster);
        victim.getWorld().spawnParticle(Particle.FIREWORK,
                victim.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0.07);
    }
}
