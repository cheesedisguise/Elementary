package dev.elementary.ability.shadow;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.util.Targets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
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
 * Vanquish: the spec sheet left this slot dark, so the shadow filled it.
 * Blink directly behind your prey, knife already raised - the next
 * melee strike within four seconds hits +5 harder (+7 at tier 2) and
 * brands the victim with 3 seconds of Fear. The empowered strike is
 * consumed by CombatListener.
 */
public class Vanquish implements dev.elementary.ability.Ability {
    /** caster -> tick the empowered window closes. */
    private static final Map<UUID, Integer> empowered = new HashMap<>();

    private final ElementaryPlugin plugin;

    public Vanquish(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Vanquish"; }
    @Override public double cooldownSeconds(int tier) { return 40; }

    /** True once: the strike spends the window. */
    public static boolean consumeEmpowered(Player attacker) {
        Integer until = empowered.remove(attacker.getUniqueId());
        return until != null && Bukkit.getCurrentTick() < until;
    }

    @Override
    public boolean cast(Player caster, int tier) {
        double range = tier >= 2 ? 16 : 12;
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult prey = caster.getWorld().rayTrace(caster.getEyeLocation(), dir,
                range, FluidCollisionMode.NEVER, true, 0.9,
                e -> e instanceof LivingEntity le && Targets.hostile(caster, le));
        if (prey == null || !(prey.getHitEntity() instanceof LivingEntity victim)) {
            Msg.fail(caster, "No prey in sight");
            return false;
        }
        Vector facing = victim.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.01) facing = new Vector(0, 0, 1);
        Location behind = victim.getLocation().clone()
                .subtract(facing.clone().normalize().multiply(1.6));
        if (behind.getBlock().getType().isSolid()) behind.add(0, 1, 0);
        if (behind.getBlock().getType().isSolid()) behind = victim.getLocation().clone();
        behind.setYaw(victim.getLocation().getYaw());
        behind.setPitch(0);
        smoke(caster.getLocation());
        caster.teleport(behind);
        caster.setFallDistance(0);
        smoke(behind);
        caster.getWorld().playSound(behind,
                Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.45f);
        caster.playSound(behind, Sound.BLOCK_SCULK_SHRIEKER_BREAK, 0.5f, 1.8f);
        empowered.put(caster.getUniqueId(), Bukkit.getCurrentTick() + 80);
        return true;
    }

    private void smoke(Location at) {
        at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 1, 0), 22,
                0.3, 0.6, 0.3, 0.02);
        at.getWorld().spawnParticle(Particle.DUST, at.clone().add(0, 1, 0), 16,
                0.35, 0.7, 0.35, 0, new Particle.DustOptions(Color.fromRGB(0x8F2030), 1.2f));
    }
}
