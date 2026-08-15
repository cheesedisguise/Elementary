package dev.elementary.ability.earth;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Fissure: punch the ground and a crack races 12 blocks along your aim,
 * climbing slopes and stopping at cliffs. Anyone caught over it is
 * erupted upward, damaged and slowed. Tremor's launch made directional
 * - a skill shot you aim, not a panic button.
 */
public class Fissure implements Ability {
    private final JavaPlugin plugin;

    public Fissure(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Fissure"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) {
            float yaw = (float) Math.toRadians(caster.getLocation().getYaw());
            dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        Vector step = dir.normalize();
        double length = tier >= 2 ? 18 : 12;
        Location cursor = caster.getLocation().clone();
        Set<UUID> caught = new HashSet<>();
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
        new BukkitRunnable() {
            double travelled = 0;
            @Override public void run() {
                for (int burst = 0; burst < 2 && travelled < length; burst++) {
                    travelled += 1;
                    cursor.add(step);
                    Block ground = surface(cursor);
                    if (ground == null) {
                        // the crack dies at the cliff edge
                        finish(cursor);
                        return;
                    }
                    cursor.setY(ground.getY() + 1);
                    Location at = cursor.clone().add(0, 0.2, 0);
                    // the ground splits open in its own material
                    cursor.getWorld().spawnParticle(Particle.BLOCK, at, 16,
                            0.45, 0.35, 0.45, ground.getBlockData());
                    cursor.getWorld().spawnParticle(Particle.BLOCK,
                            at.clone().add(0, 0.8, 0), 6, 0.25, 0.5, 0.25,
                            ground.getBlockData());
                    if ((int) travelled % 3 == 0) {
                        cursor.getWorld().playSound(at, Sound.BLOCK_STONE_BREAK,
                                0.9f, 0.55f);
                    }
                    for (LivingEntity target : at.getNearbyLivingEntities(1.6, 2.2, 1.6)) {
                        if (!Targets.hostile(caster, target)
                                || !caught.add(target.getUniqueId())) continue;
                        target.damage(5, caster);
                        target.setVelocity(target.getVelocity().clone().setY(0.85));
                        if (tier >= 2) {
                            // tier 2: the earth grips what it throws
                            target.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 40, 9));
                        } else {
                            target.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 60, 1));
                        }
                        cursor.getWorld().spawnParticle(Particle.BLOCK,
                                target.getLocation(), 24, 0.3, 0.4, 0.3,
                                ground.getBlockData());
                    }
                }
                if (travelled >= length) { finish(cursor); }
            }

            private void finish(Location end) {
                end.getWorld().spawnParticle(Particle.BLOCK, end.clone().add(0, 0.4, 0),
                        30, 0.7, 0.5, 0.7, end.clone().subtract(0, 1, 0)
                                .getBlock().getBlockData());
                end.getWorld().playSound(end, Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.4f);
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
        return true;
    }

    /** The walkable ground at this column, or null past a cliff. */
    private Block surface(Location at) {
        int x = at.getBlockX();
        int z = at.getBlockZ();
        for (int y = at.getBlockY() + 2; y > at.getBlockY() - 4; y--) {
            Block block = at.getWorld().getBlockAt(x, y, z);
            if (block.getType().isSolid()) return block;
        }
        return null;
    }
}
