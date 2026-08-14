package dev.elementary.ability.fire;

import dev.elementary.ability.Ability;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
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

public class Meteor implements Ability, Listener {
    private static final String TAG = "elementary_meteor";

    private final JavaPlugin plugin;

    public Meteor(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Meteor"; }
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
        Location spawn = target.clone().add(7, 24, 7);
        Vector dir = target.toVector().subtract(spawn.toVector()).normalize();
        Fireball meteor = caster.getWorld().spawn(spawn, Fireball.class, ball -> {
            ball.setShooter(caster);
            ball.setYield(0f);
            ball.setIsIncendiary(false);
        });
        meteor.setVelocity(dir.clone().multiply(1.8));
        meteor.setAcceleration(dir.clone().multiply(0.12));
        meteor.setMetadata(TAG, new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override public void run() {
                if (meteor.isDead()) { cancel(); return; }
                Location at = meteor.getLocation();
                at.getWorld().spawnParticle(Particle.FLAME, at, 10, 0.35, 0.35, 0.35, 0.02);
                at.getWorld().spawnParticle(Particle.LAVA, at, 3, 0.25, 0.25, 0.25, 0);
                at.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, at, 2,
                        0.15, 0.15, 0.15, 0.01);
            }
        }.runTaskTimer(plugin, 0, 1);
        return true;
    }

    @EventHandler
    public void onImpact(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.hasMetadata(TAG)) return;
        event.setCancelled(true);
        projectile.remove();
        Location at = projectile.getLocation();
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        at.getWorld().spawnParticle(Particle.DUST, at, 120, 5, 3, 5, 0,
                new Particle.DustOptions(Color.fromRGB(0xF07020), 1.4f));
        at.getWorld().playSound(at, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        for (LivingEntity target : at.getNearbyLivingEntities(6)) {
            if (target.equals(shooter)) continue;
            target.damage(12, shooter);
            target.setFireTicks(Math.max(target.getFireTicks(), 100));
        }
        burningGround(at, shooter);
    }

    /** 8s of visual-only burning ground that ignites anyone standing in it. */
    private void burningGround(Location center, Player shooter) {
        List<Location> patch = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            double angle = 2 * Math.PI * i / 26;
            double dist = 1 + (i % 4);
            patch.add(center.clone().add(dist * Math.cos(angle), 0.15, dist * Math.sin(angle)));
        }
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 4;
                if (ticks > 20 * 8) { cancel(); return; }
                for (Location spot : patch) {
                    center.getWorld().spawnParticle(Particle.SMALL_FLAME, spot, 1,
                            0.2, 0.05, 0.2, 0.005);
                }
                if (ticks % 20 == 0) {
                    for (LivingEntity target : center.getNearbyLivingEntities(4.5)) {
                        if (target.equals(shooter)) continue;
                        target.setFireTicks(Math.max(target.getFireTicks(), 40));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 4);
    }
}
