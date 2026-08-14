package dev.elementary.ability.ice;

import dev.elementary.ElementaryPlugin;
import dev.elementary.ability.Ability;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Mirage: four live-synced clones step out along the cardinals, turn
 * right, then wander until popped or the 15s lifetime ends. Rendering
 * is pluggable: packet NPCs with the caster's skin when PacketEvents is
 * installed, armour-stand mannequins otherwise.
 */
public class Mirage implements Ability {
    public interface Clone {
        void moveTo(Location location);
        Location location();
        void pop();
        void syncEquipment();
        boolean matchesEntityId(int entityId);
    }

    public interface CloneRenderer {
        Clone spawn(Player owner, Location location);
    }

    private final ElementaryPlugin plugin;
    private CloneRenderer renderer;
    private final Map<UUID, List<CloneWalker>> active = new HashMap<>();

    public Mirage(ElementaryPlugin plugin, CloneRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
    }

    public void setRenderer(CloneRenderer renderer) {
        this.renderer = renderer;
    }

    @Override public String name() { return "Mirage"; }
    @Override public double cooldownSeconds(int tier) { return tier >= 2 ? 20 : 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        List<CloneWalker> existing = active.remove(caster.getUniqueId());
        if (existing != null) {
            existing.forEach(CloneWalker::pop);
            return false; // recast dismisses the survivors, no recharge
        }
        List<CloneWalker> walkers = new ArrayList<>();
        Vector[] cardinals = {
                new Vector(0, 0, -1), new Vector(1, 0, 0),
                new Vector(0, 0, 1), new Vector(-1, 0, 0)};
        for (Vector dir : cardinals) {
            Clone clone;
            try {
                clone = renderer.spawn(caster, caster.getLocation().clone());
            } catch (Throwable t) {
                plugin.getSLF4JLogger().error(
                        "Mirage clone renderer failed - falling back to armour stands. "
                                + "Report this stacktrace:", t);
                renderer = new StandCloneRenderer();
                clone = renderer.spawn(caster, caster.getLocation().clone());
            }
            CloneWalker walker = new CloneWalker(caster, clone, dir.clone());
            walkers.add(walker);
            walker.runTaskTimer(plugin, 1, 1);
        }
        active.put(caster.getUniqueId(), walkers);
        caster.getWorld().playSound(caster.getLocation(),
                org.bukkit.Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1.3f);
        return true;
    }

    public void syncEquipment(Player owner) {
        List<CloneWalker> walkers = active.get(owner.getUniqueId());
        if (walkers == null) return;
        walkers.forEach(w -> w.clone.syncEquipment());
    }

    /** Pops the clone hit by this entity id; true if one was hit. */
    public boolean handleHit(Player attacker, int entityId) {
        for (List<CloneWalker> walkers : active.values()) {
            for (CloneWalker walker : walkers) {
                if (walker.clone.matchesEntityId(entityId)) {
                    walker.pop();
                    return true;
                }
            }
        }
        return false;
    }

    public class CloneWalker extends BukkitRunnable {
        final Player owner;
        final Clone clone;
        Vector direction;
        final Location at;
        int age = 0;
        double phaseDistance = 0;
        int phase = 0; // 0 walk out, 1 walk right, 2 wander
        int pauseTicks = 0;
        boolean popped = false;

        CloneWalker(Player owner, Clone clone, Vector direction) {
            this.owner = owner;
            this.clone = clone;
            this.direction = direction;
            this.at = owner.getLocation().clone();
        }

        void pop() {
            if (popped) return;
            popped = true;
            Location where = clone.location();
            where.getWorld().spawnParticle(Particle.CLOUD, where.clone().add(0, 1, 0), 30,
                    0.35, 0.7, 0.35, 0.02);
            where.getWorld().spawnParticle(Particle.SNOWFLAKE, where.clone().add(0, 1, 0), 8,
                    0.3, 0.6, 0.3, 0.01);
            where.getWorld().playSound(where, org.bukkit.Sound.ENTITY_PUFFER_FISH_BLOW_OUT,
                    0.9f, 1.5f);
            clone.pop();
            cancel();
            List<CloneWalker> walkers = active.get(owner.getUniqueId());
            if (walkers != null) {
                walkers.remove(this);
                if (walkers.isEmpty()) active.remove(owner.getUniqueId());
            }
        }

        @Override
        public void run() {
            age++;
            if (age > 20 * 15 || !owner.isOnline()) { pop(); return; }
            if (pauseTicks > 0) { pauseTicks--; return; }
            double step = 0.14; // a believable walking pace
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            switch (phase) {
                case 0 -> {
                    walk(step);
                    if (phaseDistance >= 4) { turnRight(); phase = 1; phaseDistance = 0; }
                }
                case 1 -> {
                    walk(step);
                    if (phaseDistance >= 3) { phase = 2; phaseDistance = 0; }
                }
                case 2 -> {
                    walk(step);
                    if (phaseDistance >= 2 + rng.nextDouble(4)) {
                        phaseDistance = 0;
                        pauseTicks = rng.nextInt(10, 35);
                        double yaw = rng.nextDouble(2 * Math.PI);
                        direction = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
                    }
                }
            }
            // trailing mist so close readers can spot the fakes
            if (age % 8 == 0) {
                at.getWorld().spawnParticle(Particle.CLOUD, at, 1, 0.1, 0.02, 0.1, 0.001);
            }
        }

        void turnRight() {
            direction = new Vector(-direction.getZ(), 0, direction.getX());
        }

        void walk(double step) {
            Location next = at.clone().add(direction.clone().multiply(step));
            // step up or down a block, refuse walls
            org.bukkit.block.Block foot = next.getBlock();
            if (foot.getType().isSolid()) {
                if (!foot.getRelative(0, 1, 0).getType().isSolid()) {
                    next.add(0, 1, 0);
                } else {
                    turnRight();
                    return;
                }
            } else if (!foot.getRelative(0, -1, 0).getType().isSolid()
                    && !foot.getRelative(0, -2, 0).getType().isSolid()) {
                turnRight(); // don't wander off cliffs
                return;
            } else if (!foot.getRelative(0, -1, 0).getType().isSolid()) {
                next.add(0, -1, 0);
            }
            at.setX(next.getX());
            at.setY(next.getY());
            at.setZ(next.getZ());
            at.setYaw((float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())));
            phaseDistance += step;
            clone.moveTo(at);
        }
    }
}
