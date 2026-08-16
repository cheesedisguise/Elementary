package dev.elementary.ability.light;

import dev.elementary.ability.ChargedAbility;
import dev.elementary.util.Targets;
import dev.elementary.util.TrueDamage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Sunspear: hold right-click and the light builds - one charge per
 * second, five in all, each one popping an expanding ring of light in
 * front of you. At five charges it fires ITSELF, and it hits like the
 * sun: solid base damage plus one TRUE damage per charge banked.
 * Release early to loose whatever you've gathered. Tier 2 pierces.
 */
public class Sunspear implements ChargedAbility {
    private static final int TICKS_PER_CHARGE = 20;
    private static final int MAX_CHARGES = 5;

    private class Draw {
        final int startTick = Bukkit.getCurrentTick();
        int lastFeed = startTick;
        int charges = 0;
        final int tier;
        Draw(int tier) { this.tier = tier; }
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Draw> drawing = new HashMap<>();

    public Sunspear(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Sunspear"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean charging(Player player) {
        return drawing.containsKey(player.getUniqueId());
    }

    @Override
    public void feed(Player player) {
        Draw draw = drawing.get(player.getUniqueId());
        if (draw != null) draw.lastFeed = Bukkit.getCurrentTick();
    }

    @Override
    public boolean cast(Player caster, int tier) {
        Draw draw = new Draw(tier);
        drawing.put(caster.getUniqueId(), draw);
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.7f);
        new BukkitRunnable() {
            @Override public void run() {
                int now = Bukkit.getCurrentTick();
                if (!caster.isOnline() || caster.isDead()) {
                    drawing.remove(caster.getUniqueId());
                    cancel();
                    return;
                }
                int due = Math.min(MAX_CHARGES,
                        (now - draw.startTick) / TICKS_PER_CHARGE);
                while (draw.charges < due) {
                    draw.charges++;
                    chargeRing(caster, draw.charges);
                }
                // five charges gathered: the spear looses itself
                if (draw.charges >= MAX_CHARGES) {
                    drawing.remove(caster.getUniqueId());
                    cancel();
                    fire(caster, draw.tier, MAX_CHARGES);
                    return;
                }
                if (now - draw.lastFeed > 6) {
                    drawing.remove(caster.getUniqueId());
                    cancel();
                    fire(caster, draw.tier, draw.charges);
                    return;
                }
                caster.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 8, 1, true, false));
                caster.getWorld().spawnParticle(Particle.END_ROD,
                        caster.getEyeLocation().add(caster.getEyeLocation()
                                .getDirection().multiply(0.8)),
                        1 + draw.charges, 0.2, 0.2, 0.2, 0.008);
            }
        }.runTaskTimer(plugin, 1, 1);
        return true;
    }

    /** One ring per charge: pops small in front of you, then expands. */
    private void chargeRing(Player caster, int n) {
        Vector dir = caster.getEyeLocation().getDirection().normalize();
        Location center = caster.getEyeLocation().clone().add(dir.clone().multiply(2.2));
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);
        right.normalize();
        Vector up = dir.clone().crossProduct(right).normalize();
        Vector r2 = right;
        caster.getWorld().playSound(caster.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 0.8f + 0.22f * n);
        if (n >= MAX_CHARGES) {
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.7f);
        }
        new BukkitRunnable() {
            double radius = 0.35;
            int age = 0;
            @Override public void run() {
                age++;
                if (age > 9) { cancel(); return; }
                for (int i = 0; i < 20; i++) {
                    double a = 2 * Math.PI * i / 20;
                    Location at = center.clone()
                            .add(r2.clone().multiply(radius * Math.cos(a)))
                            .add(up.clone().multiply(radius * Math.sin(a)));
                    center.getWorld().spawnParticle(Particle.DUST, at, 1,
                            0.02, 0.02, 0.02, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.3f));
                    if (i % 4 == 0) {
                        center.getWorld().spawnParticle(Particle.END_ROD, at, 1,
                                0.02, 0.02, 0.02, 0.002);
                    }
                }
                radius += 0.19;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void fire(Player caster, int tier, int charges) {
        double range = 24;
        double base = 4;
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
        // the sound of something that should not be pointed at you
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_ALLAY_ITEM_THROWN, 1.4f, 1.2f);
        if (charges >= 3) {
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.55f, 1.7f);
        }
        if (charges >= MAX_CHARGES) {
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            caster.getWorld().playSound(caster.getLocation(),
                    Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.8f);
            caster.getWorld().spawnParticle(Particle.FLASH,
                    caster.getEyeLocation().add(dir.clone().multiply(1.5)), 1);
        }
        int beamDensity = 1 + charges / 2;
        for (double d = 0.5; d < range; d += 0.4) {
            Location at = caster.getEyeLocation().clone().add(dir.clone().multiply(d));
            caster.getWorld().spawnParticle(Particle.END_ROD, at, beamDensity,
                    0.03, 0.03, 0.03, 0.002);
            if (charges >= 3 && ((int) (d * 2.5)) % 3 == 0) {
                caster.getWorld().spawnParticle(Particle.DUST, at, 2, 0.12, 0.12, 0.12, 0,
                        new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.2f));
            }
        }
        for (LivingEntity victim : struck) {
            boolean undead = Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType());
            victim.damage(undead ? base + 3 : base, caster);
            if (charges > 0) {
                // the banked light burns through everything: 1 true per charge
                TrueDamage.apply(victim, charges, caster);
            }
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0));
            victim.getWorld().spawnParticle(Particle.FIREWORK,
                    victim.getEyeLocation(), 8 + 5 * charges, 0.25, 0.25, 0.25, 0.09);
        }
    }
}
