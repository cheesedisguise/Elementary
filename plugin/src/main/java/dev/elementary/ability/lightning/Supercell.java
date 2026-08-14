package dev.elementary.ability.lightning;

import dev.elementary.ability.Ability;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** A personal storm: bolts hammer nearby enemies for eight seconds. */
public class Supercell implements Ability {
    private final JavaPlugin plugin;

    public Supercell(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Supercell"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 8, 1));
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 4;
                if (ticks > 20 * 8 || !caster.isOnline()) { cancel(); return; }
                org.bukkit.Location cloud = caster.getLocation().add(0, 3.5, 0);
                caster.getWorld().spawnParticle(Particle.CLOUD, cloud, 6, 1.6, 0.2, 1.6, 0.004);
                caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, cloud, 3,
                        1.2, 0.2, 1.2, 0.03);
                if (ticks % 20 == 0) {
                    List<LivingEntity> targets = new ArrayList<>();
                    for (LivingEntity target : caster.getLocation()
                            .getNearbyLivingEntities(10)) {
                        if (!target.equals(caster)) targets.add(target);
                    }
                    if (!targets.isEmpty()) {
                        LivingEntity victim = targets.get(
                                ThreadLocalRandom.current().nextInt(targets.size()));
                        victim.getWorld().strikeLightningEffect(victim.getLocation());
                        victim.damage(3, caster);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 4);
        return true;
    }
}
