package dev.elementary.ability.earth;

import dev.elementary.ability.Ability;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class Cataclysm implements Ability {
    private final JavaPlugin plugin;

    public Cataclysm(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Cataclysm"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = 10;
        List<BlockDisplay> spikes = new ArrayList<>();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 26; i++) {
            double angle = rng.nextDouble(2 * Math.PI);
            double dist = 1.5 + rng.nextDouble(radius - 1.5);
            Location at = caster.getLocation().clone()
                    .add(dist * Math.cos(angle), 0, dist * Math.sin(angle));
            at.setY(highestSolidY(at) + 1);
            BlockDisplay spike = caster.getWorld().spawn(at, BlockDisplay.class, d -> {
                d.setBlock((rng.nextBoolean() ? Material.DRIPSTONE_BLOCK : Material.STONE)
                        .createBlockData());
                d.setTransformation(new Transformation(
                        new Vector3f(-0.35f, 0f, -0.35f), new org.joml.Quaternionf(),
                        new Vector3f(0.7f, 1.8f + rng.nextFloat(), 0.7f),
                        new org.joml.Quaternionf()));
                d.setPersistent(false);
            });
            spikes.add(spike);
            caster.getWorld().spawnParticle(Particle.BLOCK, at, 25, 0.3, 0.6, 0.3,
                    Material.STONE.createBlockData());
        }
        caster.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, caster.getLocation(), 1);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.7f);
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(radius)) {
            if (!dev.elementary.util.Targets.hostile(caster, target)) continue;
            target.damage(8, caster);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9));
        }
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (BlockDisplay spike : spikes) {
                spike.getWorld().spawnParticle(Particle.BLOCK, spike.getLocation(), 10,
                        0.3, 0.5, 0.3, Material.STONE.createBlockData());
                spike.remove();
            }
        }, 60);
        return true;
    }

    private int highestSolidY(Location at) {
        int y = at.getBlockY();
        for (int i = 0; i < 12; i++) {
            if (at.getWorld().getBlockAt(at.getBlockX(), y - i, at.getBlockZ())
                    .getType().isSolid()) {
                return y - i;
            }
        }
        return at.getBlockY() - 1;
    }
}
