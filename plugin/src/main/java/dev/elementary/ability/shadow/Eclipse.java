package dev.elementary.ability.shadow;

import dev.elementary.ability.Ability;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** An 8s zone of darkness: enemies wilt inside it, the caster thrives. */
public class Eclipse implements Ability {
    private final JavaPlugin plugin;

    public Eclipse(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Eclipse"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = 9;
        caster.getWorld().playSound(center, org.bukkit.Sound.ENTITY_WITHER_AMBIENT,
                1.2f, 0.5f);
        new BukkitRunnable() {
            int ticks = 0;
            double spin = 0;
            @Override public void run() {
                ticks += 2;
                spin += 0.22;
                if (ticks > 20 * 8) { cancel(); return; }
                for (int i = 0; i < 20; i++) {
                    double angle = spin + 2 * Math.PI * i / 20;
                    center.getWorld().spawnParticle(Particle.SQUID_INK,
                            center.clone().add(radius * Math.cos(angle),
                                    0.3 + (i % 4) * 0.7, radius * Math.sin(angle)),
                            1, 0.1, 0.1, 0.1, 0.001);
                }
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0),
                        6, radius * 0.5, 1.2, radius * 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(0x4D0F18), 1.6f));
                if (ticks % 20 == 0) {
                    for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                        if (!dev.elementary.util.Targets.hostile(caster, target)) continue;
                        target.damage(1, caster);
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS, 60, 0));
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.WEAKNESS, 40, 1));
                    }
                    if (caster.getLocation().distance(center) <= radius) {
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.STRENGTH, 40, 1, true, false));
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.SPEED, 40, 1, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
