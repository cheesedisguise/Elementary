package dev.elementary.ability.air;

import dev.elementary.ability.Ability;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** True flight for eight seconds while enemies below are lifted. */
public class Tempest implements Ability {
    private final JavaPlugin plugin;

    public Tempest(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Tempest"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        boolean hadFlight = caster.getAllowFlight();
        caster.setAllowFlight(true);
        caster.setFlying(true);
        new BukkitRunnable() {
            int ticks = 0;
            double spin = 0;
            @Override public void run() {
                ticks += 2;
                spin += 0.5;
                boolean over = ticks > 20 * 8 || !caster.isOnline();
                if (over) {
                    if (caster.isOnline() && caster.getGameMode() != GameMode.CREATIVE
                            && caster.getGameMode() != GameMode.SPECTATOR) {
                        caster.setAllowFlight(hadFlight);
                        caster.setFlying(false);
                        caster.setFallDistance(0);
                        for (int i = 0; i < 20; i++) {
                            double angle = 2 * Math.PI * i / 20;
                            caster.getWorld().spawnParticle(Particle.CLOUD,
                                    caster.getLocation().clone()
                                            .add(1.6 * Math.cos(angle), 0, 1.6 * Math.sin(angle)),
                                    2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                    cancel();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    double angle = spin + i * 2 * Math.PI / 3;
                    double r = 1.4 + 0.6 * Math.sin(ticks / 8.0 + i);
                    caster.getWorld().spawnParticle(Particle.CLOUD,
                            caster.getLocation().clone().add(r * Math.cos(angle),
                                    0.3 + ((spin + i) % 2.2), r * Math.sin(angle)),
                            1, 0.05, 0.05, 0.05, 0.005);
                }
                if (ticks % 20 == 0) {
                    for (LivingEntity target : caster.getLocation()
                            .getNearbyLivingEntities(8)) {
                        if (target.equals(caster)) continue;
                        target.setVelocity(target.getVelocity().clone().setY(
                                Math.max(target.getVelocity().getY(), 0.45)));
                        target.damage(1, caster);
                        target.getWorld().spawnParticle(Particle.GUST,
                                target.getLocation(), 1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }
}
