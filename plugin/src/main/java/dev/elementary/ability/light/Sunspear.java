package dev.elementary.ability.light;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.status.StatusService;
import dev.elementary.util.Targets;
import dev.elementary.util.TrueDamage;
import java.util.ArrayList;
import java.util.List;
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
 * Sunspear: every banked Radiance stack is spent in one beam.
 * One stack is a flicker - 1 true damage. Two is a lance - 2.5 true
 * and 3s of Luminosity. From three up, each extra stack adds a full
 * heart of true damage and another 1.5s of Luminosity. Tier 2 pierces
 * everything on the line.
 */
public class Sunspear implements dev.elementary.ability.Ability {
    private final ElementaryPlugin plugin;

    public Sunspear(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Sunspear"; }
    @Override public double cooldownSeconds(int tier) { return 2; }

    @Override
    public boolean cast(Player caster, int tier) {
        int banked = plugin.radiance().stacks(caster);
        if (banked < 1) {
            Msg.fail(caster, "No Radiance banked");
            return false;
        }
        double range = 24;
        Vector dir = caster.getEyeLocation().getDirection();
        List<LivingEntity> struck = new ArrayList<>();
        if (tier >= 2) {
            for (LivingEntity target : caster.getWorld().getNearbyLivingEntities(
                    caster.getEyeLocation(), range, range, range)) {
                if (!Targets.hostile(caster, target)) continue;
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
                    e -> e instanceof LivingEntity le && Targets.hostile(caster, le));
            if (hit != null && hit.getHitEntity() instanceof LivingEntity victim) {
                struck.add(victim);
            }
        }
        int spent = plugin.radiance().spendAll(caster);
        double damage = spent <= 1 ? 1.0 : 2.5 + 2.0 * Math.max(0, spent - 2);
        int lumTicks = spent < 2 ? 0
                : (int) ((3.0 + 1.5 * Math.max(0, spent - 2)) * 20);

        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_ALLAY_ITEM_THROWN, 1.2f, 1.3f - 0.06f * spent);
        if (spent >= 3) {
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
        }
        if (spent >= MAX_FLASH) {
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.8f);
            caster.getWorld().spawnParticle(Particle.FLASH,
                    caster.getEyeLocation().add(dir.clone().multiply(1.5)), 1);
        }
        int density = 1 + spent / 2;
        for (double d = 0.5; d < range; d += 0.4) {
            Location at = caster.getEyeLocation().clone().add(dir.clone().multiply(d));
            caster.getWorld().spawnParticle(Particle.END_ROD, at, density,
                    0.03, 0.03, 0.03, 0.002);
            if (spent >= 3 && ((int) (d * 2.5)) % 3 == 0) {
                caster.getWorld().spawnParticle(Particle.DUST, at, 2, 0.12, 0.12, 0.12, 0,
                        new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.2f));
            }
        }
        for (LivingEntity victim : struck) {
            TrueDamage.apply(victim, damage, caster);
            if (lumTicks > 0) {
                plugin.status().apply(victim, StatusService.Status.LUMINOSITY,
                        lumTicks, caster);
            }
            victim.getWorld().spawnParticle(Particle.FIREWORK,
                    victim.getEyeLocation(), 6 + 4 * spent, 0.25, 0.25, 0.25, 0.08);
        }
        return true;
    }

    private static final int MAX_FLASH = 5;
}
