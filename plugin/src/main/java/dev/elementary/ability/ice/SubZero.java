package dev.elementary.ability.ice;

import dev.elementary.ability.Ability;
import dev.elementary.util.TrueDamage;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sub-Zero: everyone within ten blocks is flash-frozen for 2.5 seconds —
 * encased in an ice shell, held mid-air, frostbitten, camera locked,
 * four true damage, still hittable the whole time.
 */
public class SubZero implements Ability {
    private final JavaPlugin plugin;

    public SubZero(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Sub-Zero"; }
    @Override public double cooldownSeconds(int tier) { return 60; }
    @Override public boolean ultimate() { return true; }

    @Override
    public boolean cast(Player caster, int tier) {
        List<LivingEntity> victims = new ArrayList<>();
        for (LivingEntity target : caster.getLocation().getNearbyLivingEntities(10)) {
            if (!target.equals(caster)) victims.add(target);
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.BLOCK_GLASS_PLACE, 1.5f, 0.5f);
        for (LivingEntity victim : victims) {
            freeze(caster, victim);
        }
        return true;
    }

    private void freeze(Player caster, LivingEntity victim) {
        Location hold = victim.getLocation().clone().add(0, 0.5, 0);
        BlockDisplay shell = victim.getWorld().spawn(
                hold.clone().subtract(0, 0.1, 0), BlockDisplay.class, d -> {
                    d.setBlock(Material.ICE.createBlockData());
                    d.setTransformation(new Transformation(
                            new Vector3f(-0.6f, 0f, -0.6f), new Quaternionf(),
                            new Vector3f(1.2f, 2.2f, 1.2f), new Quaternionf()));
                    d.setPersistent(false);
                });
        victim.getWorld().spawnParticle(Particle.BLOCK, hold, 30, 0.4, 0.9, 0.4,
                Material.ICE.createBlockData());
        TrueDamage.apply(victim, 4, caster);
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                ticks++;
                if (ticks > 50 || victim.isDead()) {
                    shell.getWorld().spawnParticle(Particle.BLOCK, hold, 40, 0.5, 1, 0.5,
                            Material.ICE.createBlockData());
                    shell.getWorld().spawnParticle(Particle.SNOWFLAKE, hold, 25,
                            0.4, 0.9, 0.4, 0.05);
                    shell.getWorld().playSound(hold,
                            org.bukkit.Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                    shell.remove();
                    cancel();
                    return;
                }
                // pinned in place, view locked, vanilla frost overlay
                victim.teleport(hold);
                victim.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                victim.setFreezeTicks(Math.max(victim.getFreezeTicks(), 200));
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
