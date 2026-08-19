package dev.elementary.ability.lightning;

import dev.elementary.ElementaryPlugin;
import dev.elementary.util.Targets;
import dev.elementary.util.TrueDamage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Emotion Wave: a signal flung to the skies - then six walls of static
 * crawl outward from the cast point, one block per second, twelve
 * blocks far. Slow enough to sidestep, wide enough that six of them is
 * a problem. Each wall lands 1 true damage on whoever it washes over
 * (1.25 during Powerplant).
 */
public class EmotionWave implements dev.elementary.ability.Ability {
    private static final int BEAMS = 6;

    private final ElementaryPlugin plugin;

    public EmotionWave(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Emotion Wave"; }
    @Override public double cooldownSeconds(int tier) { return 40; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double maxRange = tier >= 2 ? 16 : 12;
        double damage = 1.0 * Powerplant.moveMultiplier(caster);

        // the signal to the skies
        for (double y = 0; y < 14; y += 0.5) {
            center.getWorld().spawnParticle(Particle.END_ROD,
                    center.clone().add(0, y, 0), 1, 0.04, 0.04, 0.04, 0.001);
        }
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.9f);
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.5f, 1.4f);

        List<Vector> dirs = new ArrayList<>();
        double base = Math.toRadians(caster.getLocation().getYaw());
        for (int i = 0; i < BEAMS; i++) {
            double a = base + 2 * Math.PI * i / BEAMS;
            dirs.add(new Vector(-Math.sin(a), 0, Math.cos(a)));
        }
        List<Set<UUID>> hitPerBeam = new ArrayList<>();
        for (int i = 0; i < BEAMS; i++) hitPerBeam.add(new HashSet<>());

        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age += 2;
                double radius = age / 20.0; // one block per second
                if (radius > maxRange) { cancel(); return; }
                for (int i = 0; i < BEAMS; i++) {
                    Location head = center.clone()
                            .add(dirs.get(i).clone().multiply(radius));
                    for (double y = 0.1; y <= 2.4; y += 0.55) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                head.clone().add(0, y, 0), 1, 0.12, 0.1, 0.12, 0.01);
                    }
                    if (age % 20 == 0) {
                        center.getWorld().playSound(head,
                                Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.6f);
                    }
                    Set<UUID> hit = hitPerBeam.get(i);
                    for (LivingEntity target : head.getNearbyLivingEntities(1.3, 2.2)) {
                        if (!Targets.hostile(caster, target)) continue;
                        if (!hit.add(target.getUniqueId())) continue;
                        TrueDamage.apply(target, damage, caster);
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                target.getEyeLocation(), 14, 0.3, 0.4, 0.3, 0.08);
                        target.getWorld().playSound(target.getLocation(),
                                Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
