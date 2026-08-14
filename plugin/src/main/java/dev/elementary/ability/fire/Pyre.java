package dev.elementary.ability.fire;

import dev.elementary.ability.Ability;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/** A fixed ring of flame that corrupts and then restores the ground. */
public class Pyre implements Ability {
    private final JavaPlugin plugin;

    public Pyre(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Pyre"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        double radius = tier >= 2 ? 8 : 5;
        Map<Block, BlockData> converted = corrupt(center, radius);
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks += 2;
                if (ticks > 20 * 10) {
                    converted.forEach((block, data) -> block.setBlockData(data, false));
                    cancel();
                    return;
                }
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    Location at = center.clone().add(radius * Math.cos(angle), 0.2,
                            radius * Math.sin(angle));
                    center.getWorld().spawnParticle(Particle.FLAME, at, 2, 0.1, 0.25, 0.1, 0.01);
                    center.getWorld().spawnParticle(Particle.SMALL_FLAME, at, 2,
                            0.25, 0.15, 0.25, 0.01);
                }
                if (ticks % 20 == 0) {
                    for (Map.Entry<Block, BlockData> e : converted.entrySet()) {
                        if (ThreadLocalRandom.current().nextInt(6) == 0) {
                            center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                                    e.getKey().getLocation().add(0.5, 1.1, 0.5), 1,
                                    0.05, 0.05, 0.05, 0.01);
                        }
                    }
                    for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                        boolean inside = target.getLocation().distance(center) <= radius;
                        if (!inside) continue;
                        if (target.equals(caster)) {
                            caster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.STRENGTH, 30, 0, true, false));
                            caster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SPEED, 30, 0, true, false));
                            if (tier >= 2) {
                                caster.addPotionEffect(new PotionEffect(
                                        PotionEffectType.REGENERATION, 30, 0, true, false));
                            }
                        } else {
                            target.damage(1, caster);
                            target.setFireTicks(Math.max(target.getFireTicks(), 40));
                            center.getWorld().spawnParticle(Particle.LAVA,
                                    target.getLocation(), 2, 0.2, 0.1, 0.2, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }

    /** Topmost solid block per column becomes Nether ground, restored later. */
    private Map<Block, BlockData> corrupt(Location center, double radius) {
        Map<Block, BlockData> converted = new HashMap<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Block top = topSolid(center, dx, dz);
                if (top == null || !convertible(top)) continue;
                converted.put(top, top.getBlockData());
                int roll = rng.nextInt(100);
                Material material = roll < 60 ? Material.NETHERRACK
                        : roll < 85 ? Material.BASALT : Material.MAGMA_BLOCK;
                top.setType(material, false);
            }
        }
        return converted;
    }

    private Block topSolid(Location center, int dx, int dz) {
        int x = center.getBlockX() + dx;
        int z = center.getBlockZ() + dz;
        for (int y = center.getBlockY(); y > center.getBlockY() - 8; y--) {
            Block block = center.getWorld().getBlockAt(x, y, z);
            if (block.getType().isSolid()) return block;
        }
        return null;
    }

    private boolean convertible(Block block) {
        Material type = block.getType();
        if (type == Material.BEDROCK || type == Material.OBSIDIAN) return false;
        if (type.name().contains("SPAWNER") || type.name().contains("SIGN")
                || type.name().contains("BED")) return false;
        BlockState state = block.getState();
        return !(state instanceof TileState);
    }
}
