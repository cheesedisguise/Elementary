package dev.elementary.ability.earth;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Boulder: rip a slab of stone out of the ground and hurl it. It flies
 * in a real arc, crunches whatever it lands on, and shoves the crowd
 * back from the crater. The fortress throws rocks.
 */
public class Boulder implements Ability, Listener {
    private static final String TAG = "elementary_boulder";

    private final JavaPlugin plugin;

    public Boulder(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Boulder"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        Location hole = caster.getLocation().clone();
        // the ground gives up its rock
        hole.getWorld().spawnParticle(Particle.BLOCK, hole, 30, 0.6, 0.3, 0.6,
                Material.COBBLESTONE.createBlockData());
        hole.getWorld().playSound(hole, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.6f);
        FallingBlock rock = caster.getWorld().spawnFallingBlock(
                caster.getEyeLocation().add(dir.clone().multiply(1.2)),
                Material.COBBLESTONE.createBlockData());
        rock.setDropItem(false);
        rock.setCancelDrop(true);
        rock.setHurtEntities(false);
        rock.setMetadata(TAG, new FixedMetadataValue(plugin, true));
        rock.setVelocity(dir.clone().multiply(1.5).add(new Vector(0, 0.28, 0)));
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age++;
                if (rock.isDead() || age > 100) {
                    if (!rock.isDead()) { impact(caster, rock.getLocation(), tier); rock.remove(); }
                    cancel();
                    return;
                }
                Location at = rock.getLocation();
                at.getWorld().spawnParticle(Particle.BLOCK, at, 3, 0.2, 0.2, 0.2,
                        rock.getBlockData());
                for (LivingEntity target : at.getNearbyLivingEntities(1.4)) {
                    if (!Targets.hostile(caster, target)) continue;
                    impact(caster, at, tier);
                    rock.remove();
                    cancel();
                    return;
                }
                if (rock.isOnGround()) {
                    impact(caster, at, tier);
                    rock.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
        return true;
    }

    private void impact(Player caster, Location at, int tier) {
        double radius = tier >= 2 ? 3.4 : 2.6;
        double damage = tier >= 2 ? 8 : 6;
        at.getWorld().spawnParticle(Particle.BLOCK, at, 45, 1.0, 0.6, 1.0,
                Material.COBBLESTONE.createBlockData());
        at.getWorld().spawnParticle(Particle.CRIT, at, 14, 0.8, 0.5, 0.8, 0.12);
        at.getWorld().playSound(at, Sound.BLOCK_DEEPSLATE_BREAK, 1.4f, 0.45f);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.4f);
        for (LivingEntity target : at.getNearbyLivingEntities(radius)) {
            if (!Targets.hostile(caster, target)) continue;
            target.damage(damage, caster);
            Vector away = target.getLocation().toVector().subtract(at.toVector()).setY(0);
            if (away.lengthSquared() < 0.01) away = new Vector(0.3, 0, 0);
            target.setVelocity(away.normalize().multiply(0.9).setY(0.35));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    /** The boulder never becomes a real block - it shatters instead. */
    @EventHandler
    public void onLand(EntityChangeBlockEvent event) {
        if (!event.getEntity().hasMetadata(TAG)) return;
        event.setCancelled(true);
        event.getEntity().remove();
    }
}
