package dev.elementary.ability.lightning;

import dev.elementary.ElementaryPlugin;
import dev.elementary.util.CameraLock;
import dev.elementary.util.Targets;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Volt Dash: five blocks forward as a living bolt. Anyone caught in
 * the dash takes 3.5 damage, their camera snaps still, and they hang
 * stunned mid-air for a full second - pinned where the current left
 * them. Powerplant raises the damage 25%.
 */
public class VoltDash implements dev.elementary.ability.Ability {
    private final ElementaryPlugin plugin;

    public VoltDash(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Volt Dash"; }
    @Override public double cooldownSeconds(int tier) { return 25; }

    @Override
    public boolean cast(Player caster, int tier) {
        double range = tier >= 2 ? 7 : 5;
        Vector dir = caster.getEyeLocation().getDirection().normalize();
        Location pos = caster.getLocation().clone();
        Set<LivingEntity> struck = new LinkedHashSet<>();
        double travelled = 0;
        while (travelled < range) {
            Location next = pos.clone().add(dir.clone().multiply(0.5));
            if (!next.getBlock().isPassable()
                    || !next.clone().add(0, 1, 0).getBlock().isPassable()) break;
            pos = next;
            travelled += 0.5;
            Location mid = pos.clone().add(0, 1, 0);
            caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mid, 6,
                    0.25, 0.45, 0.25, 0.05);
            for (LivingEntity target : mid.getNearbyLivingEntities(1.3)) {
                if (Targets.hostile(caster, target)) struck.add(target);
            }
        }
        pos.setYaw(caster.getLocation().getYaw());
        pos.setPitch(caster.getLocation().getPitch());
        caster.teleport(pos);
        caster.getWorld().playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.8f);
        caster.getWorld().playSound(pos, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2f);
        double damage = 3.5 * Powerplant.moveMultiplier(caster);
        for (LivingEntity victim : struck) {
            victim.damage(damage, caster);
            victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    victim.getEyeLocation(), 20, 0.3, 0.4, 0.3, 0.1);
            victim.getWorld().playSound(victim.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.9f);
            // hung mid-air, camera pinned, for a full second
            CameraLock.hold(plugin, victim, 20, 0.45);
        }
        return true;
    }
}
