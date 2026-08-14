package dev.elementary.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/** Particle helpers for the ability signatures. */
public final class Fx {
    private Fx() {}

    public static void ring(Location center, double radius, int points, Particle particle,
                            Object data) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location at = center.clone().add(radius * Math.cos(angle), 0.1,
                    radius * Math.sin(angle));
            if (data == null) {
                world.spawnParticle(particle, at, 1, 0, 0, 0, 0);
            } else {
                world.spawnParticle(particle, at, 1, 0, 0, 0, 0, data);
            }
        }
    }

    public static void line(Location from, Vector direction, double length, double step,
                            Particle particle, Object data) {
        World world = from.getWorld();
        Vector unit = direction.clone().normalize().multiply(step);
        Location cursor = from.clone();
        for (double d = 0; d < length; d += step) {
            cursor.add(unit);
            if (data == null) {
                world.spawnParticle(particle, cursor, 1, 0, 0, 0, 0);
            } else {
                world.spawnParticle(particle, cursor, 1, 0, 0, 0, 0, data);
            }
        }
    }

    /** Small arc of rainbow dust, red through violet. */
    public static void rainbowArc(Location center) {
        int[] colors = {0xFF3B30, 0xFF9500, 0xFFE21F, 0x39D353, 0x2FA8FF, 0x8A5CF5};
        World world = center.getWorld();
        for (int i = 0; i < colors.length; i++) {
            double angle = Math.PI * (0.15 + 0.7 * i / (colors.length - 1));
            Location at = center.clone().add(0.7 * Math.cos(angle), 0.15 * Math.sin(angle), 0);
            world.spawnParticle(Particle.DUST, at, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(colors[i]), 0.9f));
        }
        world.spawnParticle(Particle.CLOUD, center, 6, 0.25, 0.05, 0.25, 0.01);
    }
}
