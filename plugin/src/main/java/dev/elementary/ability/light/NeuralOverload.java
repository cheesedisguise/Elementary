package dev.elementary.ability.light;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.status.StatusService;
import dev.elementary.util.Targets;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Neural Overload: three Radiance stacks poured straight into someone's
 * skull. 60% chance the target is concussed for 3s; they always take 6s
 * of Luminosity. The caster always comes away in Harmony - but the
 * channel is unstable: 20% of the time it also kicks back and
 * concusses the CASTER for 5s. High risk, high tempo.
 */
public class NeuralOverload implements dev.elementary.ability.Ability {
    public static final int COST = 3;

    private final ElementaryPlugin plugin;

    public NeuralOverload(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Neural Overload"; }
    @Override public double cooldownSeconds(int tier) { return 12; }

    @Override
    public boolean cast(Player caster, int tier) {
        if (plugin.radiance().stacks(caster) < COST) {
            Msg.fail(caster, "Needs " + COST + " Radiance");
            return false;
        }
        double range = tier >= 2 ? 22 : 18;
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir,
                range, FluidCollisionMode.NEVER, true, 0.9,
                e -> e instanceof LivingEntity le && Targets.hostile(caster, le));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity victim)) {
            Msg.fail(caster, "No mind in sight");
            return false;
        }
        plugin.radiance().spend(caster, COST);

        // a jagged thread of light from eyes to eyes
        Vector to = victim.getEyeLocation().toVector()
                .subtract(caster.getEyeLocation().toVector());
        double length = to.length();
        to.normalize();
        for (double d = 0.4; d < length; d += 0.5) {
            Location at = caster.getEyeLocation().clone().add(to.clone().multiply(d))
                    .add((ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3);
            caster.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0xFFF3A8), 1.1f));
        }
        victim.getWorld().spawnParticle(Particle.ENCHANT,
                victim.getEyeLocation(), 40, 0.4, 0.4, 0.4, 1.2);
        victim.getWorld().playSound(victim.getLocation(),
                Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.6f);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 0.7f);

        plugin.status().apply(victim, StatusService.Status.LUMINOSITY, 6 * 20, caster);
        if (ThreadLocalRandom.current().nextDouble() < 0.60) {
            plugin.status().apply(victim, StatusService.Status.CONCUSSION, 3 * 20, caster);
        }
        // the overload always leaves the caster in Harmony...
        plugin.status().apply(caster, StatusService.Status.HARMONY, 8 * 20, caster);
        if (ThreadLocalRandom.current().nextDouble() < 0.20) {
            // ...but sometimes the channel kicks back too
            plugin.status().apply(caster, StatusService.Status.CONCUSSION, 5 * 20, caster);
            caster.playSound(caster.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.4f);
        }
        return true;
    }
}
