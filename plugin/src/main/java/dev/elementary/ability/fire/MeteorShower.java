package dev.elementary.ability.fire;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Meteor Shower: mark a zone at the crosshair and the sky opens - eight
 * meteors hammer it over four seconds. Each rock is survivable; standing
 * in the zone while it falls is not. The ember ring telegraphs one
 * second before the first impact, so it zones as much as it kills.
 */
public class MeteorShower implements Ability, Listener {
    private static final String TAG = "elementary_meteor";
    private static final double ZONE = 6;
    private static final int ROCKS = 8;

    private final JavaPlugin plugin;

    public MeteorShower(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Meteor Shower"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        RayTraceResult ray = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(), 30, FluidCollisionMode.NEVER, true);
        Location target = ray != null
                ? ray.getHitPosition().toLocation(caster.getWorld())
                : caster.getEyeLocation().add(
                        caster.getEyeLocation().getDirection().multiply(30));
        caster.getWorld().playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.4f);
        new BukkitRunnable() {
            int ticks = 0;
            int fallen = 0;
            @Override public void run() {
                ticks += 2;
                // the ember ring: one second of warning before the sky falls
                for (int i = 0; i < 22; i++) {
                    double angle = 2 * Math.PI * i / 22;
                    target.getWorld().spawnParticle(Particle.DUST,
                            target.clone().add(ZONE * Math.cos(angle), 0.3,
                                    ZONE * Math.sin(angle)), 1, 0.1, 0.05, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(0xF07020), 1.3f));
                }
                if (ticks % 4 == 0) {
                    target.getWorld().spawnParticle(Particle.LAVA,
                            target.clone().add(0, 0.3, 0), 2, ZONE * 0.5, 0.1, ZONE * 0.5, 0);
                }
                if (ticks >= 20 && (ticks - 20) % 10 == 0 && fallen < ROCKS) {
                    fallen++;
                    drop(caster, target);
                }
                if (fallen >= ROCKS && ticks > 20 + ROCKS * 10) cancel();
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }

    private void drop(Player caster, Location zone) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(2 * Math.PI);
        double dist = rng.nextDouble(ZONE - 1);
        Location impact = zone.clone().add(dist * Math.cos(angle), 0, dist * Math.sin(angle));
        Location spawn = impact.clone().add(rng.nextDouble(-4, 4), 22, rng.nextDouble(-4, 4));
        Vector dir = impact.toVector().subtract(spawn.toVector()).normalize();
        Fireball rock = caster.getWorld().spawn(spawn, Fireball.class, ball -> {
            ball.setShooter(caster);
            ball.setYield(0f);
            ball.setIsIncendiary(false);
        });
        rock.setVelocity(dir.clone().multiply(2.2));
        rock.setAcceleration(dir.clone().multiply(0.1));
        rock.setMetadata(TAG, new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override public void run() {
                if (rock.isDead()) { cancel(); return; }
                Location at = rock.getLocation();
                at.getWorld().spawnParticle(Particle.FLAME, at, 6, 0.25, 0.25, 0.25, 0.02);
                at.getWorld().spawnParticle(Particle.LAVA, at, 2, 0.2, 0.2, 0.2, 0);
                at.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, at, 1,
                        0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onImpact(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata(TAG)) return;
        event.setCancelled(true);
        projectile.remove();
        Location at = projectile.getLocation();
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().spawnParticle(Particle.DUST, at, 40, 2.2, 1.5, 2.2, 0,
                new Particle.DustOptions(Color.fromRGB(0xF07020), 1.4f));
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        for (LivingEntity target : at.getNearbyLivingEntities(3)) {
            if (shooter != null && !Targets.hostile(shooter, target)) continue;
            target.damage(5, shooter);
            target.setFireTicks(Math.max(target.getFireTicks(), 60));
        }
        embers(at, shooter);
    }

    /** A small patch of burning ground where each rock lands. */
    private void embers(Location center, Player shooter) {
        List<Location> patch = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double angle = 2 * Math.PI * i / 12;
            double dist = 0.6 + (i % 3);
            patch.add(center.clone().add(dist * Math.cos(angle), 0.15,
                    dist * Math.sin(angle)));
        }
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 4;
                if (ticks > 20 * 4) { cancel(); return; }
                for (Location spot : patch) {
                    center.getWorld().spawnParticle(Particle.SMALL_FLAME, spot, 1,
                            0.2, 0.05, 0.2, 0.005);
                }
                if (ticks % 20 == 0) {
                    for (LivingEntity target : center.getNearbyLivingEntities(3)) {
                        if (shooter != null && !Targets.hostile(shooter, target)) continue;
                        target.setFireTicks(Math.max(target.getFireTicks(), 40));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 4);
    }
}
