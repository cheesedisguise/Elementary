package dev.elementary.ability.shadow;

import dev.elementary.ability.Ability;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Vanish and reappear along your look direction - or, aimed at prey,
 *  directly at their back. */
public class Shadowstep implements Ability {
    private final JavaPlugin plugin;

    public Shadowstep(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Shadowstep"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        double range = tier >= 2 ? 14 : 8;
        Vector dir = caster.getEyeLocation().getDirection();
        // aimed at someone? step out of their shadow instead
        double stalkRange = tier >= 2 ? 16 : 10;
        RayTraceResult prey = caster.getWorld().rayTrace(caster.getEyeLocation(), dir,
                stalkRange, FluidCollisionMode.NEVER, true, 0.9,
                e -> e instanceof LivingEntity le
                        && dev.elementary.util.Targets.hostile(caster, le));
        if (prey != null && prey.getHitEntity() instanceof LivingEntity victim) {
            Vector facing = victim.getLocation().getDirection().setY(0);
            if (facing.lengthSquared() < 0.01) facing = new Vector(0, 0, 1);
            Location behind = victim.getLocation().clone()
                    .subtract(facing.clone().normalize().multiply(1.6));
            if (behind.getBlock().getType().isSolid()) behind.add(0, 1, 0);
            if (behind.getBlock().getType().isSolid()) behind = victim.getLocation().clone();
            // arrive facing their back, knife-ready
            behind.setYaw(victim.getLocation().getYaw());
            behind.setPitch(0);
            smoke(caster.getLocation());
            caster.teleport(behind);
            caster.setFallDistance(0);
            smoke(behind);
            caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0));
            caster.getWorld().playSound(behind,
                    org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            return true;
        }
        RayTraceResult ray = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), dir,
                range, FluidCollisionMode.NEVER, true);
        double distance = ray != null
                ? Math.max(0, ray.getHitPosition().distance(
                        caster.getEyeLocation().toVector()) - 0.8)
                : range;
        Location target = caster.getLocation().add(dir.clone().multiply(distance));
        // settle on stable footing: nudge up out of blocks, drop to ground
        while (target.getBlock().getType().isSolid() && target.getY() < caster.getWorld()
                .getMaxHeight()) {
            target.add(0, 1, 0);
        }
        smoke(caster.getLocation());
        target.setDirection(dir);
        caster.teleport(target);
        caster.setFallDistance(0);
        smoke(target);
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0));
        caster.getWorld().playSound(target, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
                0.8f, 0.6f);
        return true;
    }

    private void smoke(Location at) {
        at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 1, 0), 22,
                0.3, 0.6, 0.3, 0.02);
        at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0, 1, 0), 16,
                0.35, 0.7, 0.35, 0, new Particle.DustOptions(Color.fromRGB(0x8F2030), 1.2f));
    }
}
