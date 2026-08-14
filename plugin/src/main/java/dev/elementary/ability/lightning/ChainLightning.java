package dev.elementary.ability.lightning;

import dev.elementary.ability.Ability;
import dev.elementary.msg.Msg;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Strike one target and jump to their neighbours, decaying each hop. */
public class ChainLightning implements Ability {
    private final JavaPlugin plugin;

    public ChainLightning(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Chain Lightning"; }
    @Override public double cooldownSeconds(int tier) { return 35; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir, 12,
                FluidCollisionMode.NEVER, true, 0.8,
                e -> e instanceof LivingEntity && !e.equals(caster));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity first)) {
            Msg.fail(caster, "No target to chain from");
            return false;
        }
        int maxHops = tier >= 2 ? 5 : 3;
        List<LivingEntity> struck = new ArrayList<>();
        LivingEntity current = first;
        double damage = 5;
        spark(caster.getEyeLocation().toVector(), current.getEyeLocation().toVector(),
                caster);
        while (current != null && struck.size() <= maxHops) {
            struck.add(current);
            current.damage(damage, caster);
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    current.getEyeLocation(), 16, 0.3, 0.4, 0.3, 0.1);
            damage = Math.max(2, damage - 1);
            LivingEntity next = null;
            double best = 6;
            for (LivingEntity candidate : current.getLocation().getNearbyLivingEntities(6)) {
                if (candidate.equals(caster) || struck.contains(candidate)) continue;
                double dist = candidate.getLocation().distance(current.getLocation());
                if (dist < best) { best = dist; next = candidate; }
            }
            if (next != null) {
                spark(current.getEyeLocation().toVector(),
                        next.getEyeLocation().toVector(), caster);
            }
            current = next;
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);
        return true;
    }

    private void spark(Vector from, Vector to, Player caster) {
        Vector step = to.clone().subtract(from);
        double length = step.length();
        step.normalize().multiply(0.4);
        Vector cursor = from.clone();
        for (double d = 0; d < length; d += 0.4) {
            cursor.add(step);
            caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    cursor.toLocation(caster.getWorld()), 1, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
