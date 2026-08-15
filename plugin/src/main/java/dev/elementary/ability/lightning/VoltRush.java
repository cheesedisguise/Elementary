package dev.elementary.ability.lightning;

import dev.elementary.ElementaryPlugin;
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
 * Volt Rush: the caster becomes the bolt - an instant dash along the
 * aim line, straight through anyone in the way. Everyone passed through
 * is shocked, and touching at least one enemy snaps Momentum to full.
 */
public class VoltRush implements dev.elementary.ability.Ability {
    private final ElementaryPlugin plugin;

    public VoltRush(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Volt Rush"; }
    @Override public double cooldownSeconds(int tier) { return 20; }

    @Override
    public boolean cast(Player caster, int tier) {
        double range = tier >= 2 ? 10 : 7;
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
            caster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mid, 5,
                    0.25, 0.45, 0.25, 0.05);
            for (LivingEntity target : mid.getNearbyLivingEntities(1.3)) {
                if (Targets.hostile(caster, target)) struck.add(target);
            }
        }
        pos.setYaw(caster.getLocation().getYaw());
        pos.setPitch(caster.getLocation().getPitch());
        caster.teleport(pos);
        caster.getWorld().playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.7f);
        caster.getWorld().playSound(pos, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2f);
        for (LivingEntity victim : struck) {
            victim.damage(3, caster);
            victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    victim.getEyeLocation(), 18, 0.3, 0.4, 0.3, 0.1);
        }
        if (!struck.isEmpty()) {
            // passing through a body charges you: Momentum snaps to max
            plugin.passives().maxMomentum(caster);
        }
        return true;
    }
}
