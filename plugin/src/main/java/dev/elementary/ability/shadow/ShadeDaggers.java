package dev.elementary.ability.shadow;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.util.Targets;
import dev.elementary.util.TrueDamage;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shade Daggers: three short blades of shadow condense above the
 * caster's head (four at tier 2), hang for one second, then hunt the
 * marked target - curving mid-air to track. Each lands 1.5 true
 * damage; a raised shield shatters a dagger harmlessly. Unspent
 * daggers dissolve after 7.5 seconds.
 */
public class ShadeDaggers implements dev.elementary.ability.Ability {
    private static final int HOVER_TICKS = 20;
    private static final int LIFE_TICKS = 150; // 7.5s

    private final ElementaryPlugin plugin;

    public ShadeDaggers(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Shade Daggers"; }
    @Override public double cooldownSeconds(int tier) { return 35; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir,
                24, FluidCollisionMode.NEVER, true, 1.0,
                e -> e instanceof LivingEntity le && Targets.hostile(caster, le));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity victim)) {
            Msg.fail(caster, "No prey in sight");
            return false;
        }
        int count = tier >= 2 ? 4 : 3;
        List<ItemDisplay> daggers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) / 2.0) * 0.7;
            Location at = caster.getLocation().clone().add(spread, 2.6, 0);
            ItemDisplay dagger = caster.getWorld().spawn(at, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
                d.setTransformation(new Transformation(
                        new Vector3f(), new Quaternionf(),
                        new Vector3f(0.62f, 0.62f, 0.62f), new Quaternionf()));
                d.setPersistent(false);
            });
            daggers.add(dagger);
        }
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_SCULK_SHRIEKER_BREAK, 0.8f, 1.4f);
        drive(caster, victim, daggers);
        return true;
    }

    private void drive(Player caster, LivingEntity victim, List<ItemDisplay> daggers) {
        List<Vector> velocities = new ArrayList<>();
        for (int i = 0; i < daggers.size(); i++) velocities.add(null);
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age++;
                boolean anyAlive = false;
                for (int i = 0; i < daggers.size(); i++) {
                    ItemDisplay dagger = daggers.get(i);
                    if (!dagger.isValid()) continue;
                    anyAlive = true;
                    int launchAt = HOVER_TICKS + i * 3; // staggered release
                    if (age > LIFE_TICKS || !victim.isValid() || victim.isDead()) {
                        fizzle(dagger);
                        continue;
                    }
                    if (age < launchAt) {
                        hover(caster, dagger, i, daggers.size(), age);
                        continue;
                    }
                    Vector vel = velocities.get(i);
                    Vector toTarget = victim.getEyeLocation().toVector()
                            .subtract(dagger.getLocation().toVector());
                    if (toTarget.lengthSquared() < 0.001) toTarget = new Vector(0, -1, 0);
                    if (vel == null) {
                        vel = toTarget.clone().normalize();
                        dagger.getWorld().playSound(dagger.getLocation(),
                                Sound.ENTITY_ARROW_SHOOT, 0.9f, 0.6f);
                    } else {
                        // curve toward the mark: blend, never snap
                        vel = vel.multiply(0.65)
                                .add(toTarget.clone().normalize().multiply(0.35))
                                .normalize();
                    }
                    velocities.set(i, vel);
                    Location next = dagger.getLocation().add(vel.clone().multiply(0.75));
                    point(dagger, vel);
                    dagger.teleport(next);
                    dagger.getWorld().spawnParticle(Particle.DUST, next, 2,
                            0.06, 0.06, 0.06, 0,
                            new Particle.DustOptions(Color.fromRGB(0x8F2030), 1.0f));
                    if (age % 3 == 0) {
                        dagger.getWorld().spawnParticle(Particle.SMOKE, next, 1,
                                0.04, 0.04, 0.04, 0.005);
                    }
                    if (!next.getBlock().isPassable()) {
                        fizzle(dagger);
                        continue;
                    }
                    if (next.distanceSquared(victim.getEyeLocation()) < 1.4
                            || next.distanceSquared(victim.getLocation()) < 1.4) {
                        strike(caster, victim, dagger);
                    }
                }
                if (!anyAlive) cancel();
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    /** The waiting blades circle slowly over the caster's head. */
    private void hover(Player caster, ItemDisplay dagger, int index, int count, int age) {
        double angle = 2 * Math.PI * index / count + age * 0.12;
        Location at = caster.getLocation().clone()
                .add(0.8 * Math.cos(angle), 2.5 + 0.12 * Math.sin(age * 0.3),
                        0.8 * Math.sin(angle));
        point(dagger, new Vector(0, -0.25, 0).add(at.clone().subtract(
                caster.getLocation()).toVector().setY(0).multiply(0.2)));
        dagger.teleport(at);
        if (age % 4 == 0) {
            dagger.getWorld().spawnParticle(Particle.SMOKE, at, 1, 0.05, 0.05, 0.05, 0.004);
        }
    }

    /** Roll the display so the blade leads along its motion. */
    private void point(ItemDisplay dagger, Vector dir) {
        Vector d = dir.clone().normalize();
        float yaw = (float) Math.atan2(-d.getX(), d.getZ());
        float pitch = (float) Math.asin(-d.getY());
        Quaternionf rot = new Quaternionf()
                .rotateY(yaw)
                .rotateX(pitch + (float) Math.PI / 2f)
                .rotateZ((float) (-Math.PI / 4));
        Transformation t = dagger.getTransformation();
        dagger.setTransformation(new Transformation(
                t.getTranslation(), rot, t.getScale(), new Quaternionf()));
    }

    private void strike(Player caster, LivingEntity victim, ItemDisplay dagger) {
        Location at = dagger.getLocation();
        if (victim instanceof Player p && p.isBlocking()) {
            // the shield answers: the blade shatters into smoke
            at.getWorld().playSound(at, Sound.ITEM_SHIELD_BLOCK, 1f, 1.1f);
            at.getWorld().spawnParticle(Particle.CRIT, at, 10, 0.15, 0.15, 0.15, 0.08);
        } else {
            TrueDamage.apply(victim, 1.5, caster);
            at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.6f);
            at.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 10,
                    0.25, 0.25, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(0x8F2030), 1.3f));
        }
        dagger.remove();
    }

    private void fizzle(ItemDisplay dagger) {
        dagger.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                dagger.getLocation(), 6, 0.1, 0.1, 0.1, 0.01);
        dagger.remove();
    }
}
