package dev.elementary.ability.air;

import dev.elementary.ability.Ability;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Updraft implements Ability {
    private final JavaPlugin plugin;

    public Updraft(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Updraft"; }
    @Override public double cooldownSeconds(int tier) { return 25; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 9 : 6;
        caster.getWorld().spawnParticle(Particle.GUST, caster.getLocation(), 1);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_BREEZE_SHOOT, 1f, 1f);
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (target.equals(caster)) continue;
            target.setVelocity(target.getVelocity().clone().setY(1.0)); // ~5 blocks
            new BukkitRunnable() {
                int ticks = 0;
                @Override public void run() {
                    ticks++;
                    if (ticks > 10 || target.isDead()) { cancel(); return; }
                    target.getWorld().spawnParticle(Particle.CLOUD,
                            target.getLocation().subtract(0, 0.3, 0), 4, 0.2, 0.1, 0.2, 0.02);
                }
            }.runTaskTimer(plugin, 0, 1);
        }
        return true;
    }
}
