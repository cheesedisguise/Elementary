package dev.elementary.ability.light;

import dev.elementary.ability.Ability;
import dev.elementary.msg.Msg;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** A lance of light: instant ray, first target struck (all, at tier 2). */
public class Sunspear implements Ability {
    private final JavaPlugin plugin;

    public Sunspear(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Sunspear"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        double range = 20;
        List<LivingEntity> struck = new ArrayList<>();
        if (tier >= 2) {
            for (LivingEntity target : caster.getWorld().getNearbyLivingEntities(
                    caster.getEyeLocation(), range, range, range)) {
                if (target.equals(caster)) continue;
                Vector to = target.getEyeLocation().toVector()
                        .subtract(caster.getEyeLocation().toVector());
                double along = to.dot(dir);
                if (along < 0 || along > range) continue;
                double off = to.clone().subtract(dir.clone().multiply(along)).length();
                if (off <= 1.0) struck.add(target);
            }
        } else {
            RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir,
                    range, FluidCollisionMode.NEVER, true, 0.7,
                    e -> e instanceof LivingEntity && !e.equals(caster));
            if (hit != null && hit.getHitEntity() instanceof LivingEntity victim) {
                struck.add(victim);
            }
        }
        if (struck.isEmpty()) {
            Msg.fail(caster, "No target in the beam");
            return false;
        }
        for (double d = 0.5; d < range; d += 0.4) {
            caster.getWorld().spawnParticle(Particle.END_ROD,
                    caster.getEyeLocation().clone().add(dir.clone().multiply(d)), 1,
                    0.02, 0.02, 0.02, 0);
        }
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_ALLAY_ITEM_THROWN, 1.4f, 1.6f);
        for (LivingEntity victim : struck) {
            double damage = victim instanceof Monster ? 7 : 5;
            victim.damage(damage, caster);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0));
            victim.getWorld().spawnParticle(Particle.FIREWORK,
                    victim.getEyeLocation(), 12, 0.25, 0.25, 0.25, 0.08);
        }
        return true;
    }
}
