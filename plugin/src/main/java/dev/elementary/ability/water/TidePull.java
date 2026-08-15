package dev.elementary.ability.water;

import dev.elementary.ability.Ability;
import dev.elementary.msg.Msg;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TidePull implements Ability {
    private final JavaPlugin plugin;

    public TidePull(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Tide Pull"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir, 15,
                FluidCollisionMode.NEVER, true, 0.6,
                e -> e instanceof LivingEntity && !e.equals(caster));
        if (hit == null) {
            Msg.fail(caster, "Nothing within reach");
            return false;
        }
        double length = hit.getHitPosition().distance(caster.getEyeLocation().toVector());
        for (double d = 0.5; d < length; d += 0.5) {
            org.bukkit.Location at = caster.getEyeLocation().clone()
                    .add(dir.clone().multiply(d));
            caster.getWorld().spawnParticle(Particle.DRIPPING_WATER, at, 2, 0.05, 0.05, 0.05, 0);
            caster.getWorld().spawnParticle(Particle.BUBBLE_POP, at, 1, 0.05, 0.05, 0.05, 0);
        }
        if (hit.getHitEntity() instanceof LivingEntity victim) {
            Vector pull = caster.getLocation().toVector()
                    .subtract(victim.getLocation().toVector()).normalize()
                    .multiply(1.4).setY(0.4);
            victim.setVelocity(pull);
            caster.getWorld().spawnParticle(Particle.SPLASH, victim.getLocation(), 25,
                    0.4, 0.4, 0.4, 0);
            if (tier >= 2) { // hits up to 3 targets: drag nearby company along
                int extra = 0;
                for (LivingEntity other : victim.getLocation().getNearbyLivingEntities(3)) {
                    if (other.equals(victim) || extra >= 2
                            || !dev.elementary.util.Targets.hostile(caster, other)) continue;
                    other.setVelocity(caster.getLocation().toVector()
                            .subtract(other.getLocation().toVector()).normalize()
                            .multiply(1.4).setY(0.4));
                    extra++;
                }
            }
        } else {
            Vector jump = hit.getHitPosition().subtract(caster.getLocation().toVector())
                    .normalize().multiply(1.6).setY(Math.max(0.4,
                            (hit.getHitPosition().getY() - caster.getLocation().getY()) * 0.18));
            caster.setVelocity(jump);
            caster.setFallDistance(0);
            caster.getWorld().spawnParticle(Particle.SPLASH,
                    hit.getHitPosition().toLocation(caster.getWorld()), 25, 0.4, 0.4, 0.4, 0);
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.8f);
        return true;
    }
}
