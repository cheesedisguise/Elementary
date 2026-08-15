package dev.elementary.ability.ice;

import dev.elementary.ability.Ability;
import dev.elementary.util.GroundCover;
import dev.elementary.util.Targets;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Frozen Over: the floor freezes outward to 15 blocks - a patchwork of
 * ice, packed ice and blue ice with real vanilla slide physics. Enemies
 * skid; the caster skates it at Speed III. When it ends, the border
 * thaws back in until nothing is left.
 */
public class FrozenOver implements Ability {
    private static final double RADIUS = 15;

    private final JavaPlugin plugin;

    public FrozenOver(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Frozen Over"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        Location center = caster.getLocation().clone();
        int holdTicks = tier >= 2 ? 20 * 12 : 20 * 8;
        GroundCover ice = GroundCover.spread(plugin, center, RADIUS, 1.1, rng -> {
            int roll = rng.nextInt(100);
            return roll < 55 ? Material.ICE
                    : roll < 85 ? Material.PACKED_ICE : Material.BLUE_ICE;
        });
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_PLACE, 1.5f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.7f);
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age += 5;
                if (age > holdTicks || !caster.isOnline()) {
                    ice.recede(1.2);
                    center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 1.6f);
                    cancel();
                    return;
                }
                // the field glitters and creaks while it lasts
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < 6; i++) {
                    double angle = rng.nextDouble(2 * Math.PI);
                    double dist = rng.nextDouble(RADIUS);
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            center.clone().add(dist * Math.cos(angle), 1.0,
                                    dist * Math.sin(angle)), 1, 0.2, 0.4, 0.2, 0.005);
                }
                if (age % 40 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_GLASS_STEP, 0.5f, 0.6f);
                }
                for (LivingEntity target : center.getNearbyLivingEntities(RADIUS + 1)) {
                    Location under = target.getLocation().subtract(0, 0.4, 0);
                    if (!ice.isCovered(under.getBlock())) continue;
                    if (target.equals(caster)) {
                        // the one skater on the rink
                        caster.addPotionEffect(new PotionEffect(
                                PotionEffectType.SPEED, 20, 2, true, false));
                    } else if (tier >= 2 && Targets.hostile(caster, target)) {
                        // tier 2: the cold seeps up through their boots
                        target.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 20, 0, true, false));
                        target.setFreezeTicks(Math.min(84, target.getFreezeTicks() + 30));
                    }
                }
            }
        }.runTaskTimer(plugin, 2, 5);
        return true;
    }
}
