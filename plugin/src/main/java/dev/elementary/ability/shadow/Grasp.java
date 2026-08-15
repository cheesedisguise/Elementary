package dev.elementary.ability.shadow;

import dev.elementary.ability.Ability;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** Shadow tendrils root and wither everyone close. */
public class Grasp implements Ability {
    private final JavaPlugin plugin;

    public Grasp(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Grasp"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 8 : 5;
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (!dev.elementary.util.Targets.hostile(caster, target)) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0));
            tendril(target);
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.9f, 0.5f);
        return true;
    }

    private void tendril(LivingEntity target) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 2;
                if (ticks > 40 || target.isDead()) { cancel(); return; }
                double height = (ticks / 40.0) * 1.8;
                target.getWorld().spawnParticle(Particle.DUST,
                        target.getLocation().add(0, height, 0), 4, 0.25, 0.1, 0.25, 0,
                        new Particle.DustOptions(Color.fromRGB(0x4D0F18), 1.4f));
                target.getWorld().spawnParticle(Particle.SQUID_INK,
                        target.getLocation().add(0, 0.2, 0), 1, 0.2, 0.05, 0.2, 0.005);
            }
        }.runTaskTimer(plugin, 0, 2);
    }
}
