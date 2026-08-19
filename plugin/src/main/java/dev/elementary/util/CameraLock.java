package dev.elementary.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Pins an entity where it stands: position, yaw and pitch resent every
 * tick, velocity zeroed. For players the stream of teleports freezes
 * the camera; two ticks is a screen-lock flicker, twenty is a mid-air
 * stun. Lift raises the hold point so a stun visibly hangs its victim.
 */
public final class CameraLock {
    private CameraLock() {}

    public static void hold(JavaPlugin plugin, LivingEntity victim, int ticks, double lift) {
        Location freeze = victim.getLocation().clone().add(0, lift, 0);
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age++;
                if (age > ticks || victim.isDead() || !victim.isValid()) {
                    cancel();
                    return;
                }
                victim.teleport(freeze);
                victim.setVelocity(new Vector(0, 0, 0));
                victim.setFallDistance(0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
