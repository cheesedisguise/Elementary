package dev.elementary.ability.lightning;

import dev.elementary.ability.Ability;
import dev.elementary.msg.Msg;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** An instant electric arc: shock the first thing in the beam. */
public class Arc implements Ability {
    private final JavaPlugin plugin;

    public Arc(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Arc"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir, 16,
                FluidCollisionMode.NEVER, true, 0.7,
                e -> e instanceof LivingEntity && !e.equals(caster));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity victim)) {
            Msg.fail(caster, "No target in the arc");
            return false;
        }
        double length = hit.getHitPosition().distance(caster.getEyeLocation().toVector());
        for (double d = 0.4; d < length; d += 0.35) {
            // jittered so the beam crackles instead of drawing a straight line
            caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    caster.getEyeLocation().clone().add(dir.clone().multiply(d)), 1,
                    0.12, 0.12, 0.12, 0.01);
        }
        victim.damage(4, caster);
        int slow = tier >= 2 ? 80 : 40;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 1));
        victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                victim.getEyeLocation(), 20, 0.3, 0.4, 0.3, 0.08);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_BEE_STING, 1.2f, 0.6f);
        return true;
    }
}
