package dev.elementary.ability.water;

import dev.elementary.ability.Ability;
import dev.elementary.util.TrueDamage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** A spinning whirlpool fixed at the cast position for six seconds. */
public class Maelstrom implements Ability {
    private final JavaPlugin plugin;

    public Maelstrom(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Maelstrom"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = 12;
        new BukkitRunnable() {
            int ticks = 0;
            double spin = 0;
            @Override public void run() {
                ticks++;
                if (ticks > 20 * 6) { cancel(); return; }
                spin += 0.35;
                // two helical arms tightening toward the centre
                for (int arm = 0; arm < 2; arm++) {
                    double offset = arm * Math.PI;
                    for (int i = 0; i < 5; i++) {
                        double t = i / 5.0;
                        double r = radius * (1 - t) * (0.35 + 0.65 * ((ticks % 40) / 40.0 + t) % 1);
                        double angle = spin + offset + t * 4;
                        Location at = center.clone().add(r * Math.cos(angle),
                                0.2 + t * 1.2, r * Math.sin(angle));
                        center.getWorld().spawnParticle(Particle.SPLASH, at, 3, 0.1, 0.05, 0.1, 0);
                        center.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, at, 1,
                                0.05, 0.05, 0.05, 0);
                    }
                }
                // nautilus particles streaming inward along the pull
                double angle = spin * 1.7;
                Location edge = center.clone().add(radius * 0.8 * Math.cos(angle), 0.6,
                        radius * 0.8 * Math.sin(angle));
                Vector inward = center.toVector().subtract(edge.toVector()).normalize();
                center.getWorld().spawnParticle(Particle.NAUTILUS, edge, 0,
                        inward.getX(), inward.getY(), inward.getZ(), 0.6);

                for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                    if (target.equals(caster)) continue;
                    Vector pull = center.toVector().subtract(target.getLocation().toVector());
                    double dist = Math.max(pull.length(), 0.5);
                    target.setVelocity(target.getVelocity()
                            .add(pull.normalize().multiply(Math.min(0.55, 2.5 / dist))));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                            30, 2, true, false));
                    if (ticks % 20 == 0) {
                        TrueDamage.apply(target, 1, caster);
                        target.getWorld().spawnParticle(Particle.BUBBLE_POP,
                                target.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
        caster.getWorld().playSound(center, org.bukkit.Sound.AMBIENT_UNDERWATER_LOOP, 2f, 0.5f);
        return true;
    }
}
