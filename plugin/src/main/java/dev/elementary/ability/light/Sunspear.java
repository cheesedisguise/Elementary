package dev.elementary.ability.light;

import dev.elementary.ability.ChargedAbility;
import dev.elementary.util.Targets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
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
 * Sunspear, drawn like a bow: hold right-click to charge the lance.
 * A tap is a stab of light; a full draw is the real spear - more
 * damage, more reach, and a heavy smite bonus against the undead.
 * Fires the moment the button is released. Tier 2 pierces everything
 * in the line.
 */
public class Sunspear implements ChargedAbility {
    private static final int FULL_DRAW_TICKS = 30;   // 1.5s to full power
    private static final int AUTO_FIRE_TICKS = 60;   // held too long: loose

    private class Draw {
        final int startTick = Bukkit.getCurrentTick();
        int lastFeed = startTick;
        final int tier;
        boolean chimed = false;
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
                Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        new BukkitRunnable() {
            @Override public void run() {
                int now = Bukkit.getCurrentTick();
                if (!caster.isOnline() || caster.isDead()) {
                    drawing.remove(caster.getUniqueId());
                    cancel();
                    return;
                }
                int held = draw.lastFeed - draw.startTick;
                if (now - draw.lastFeed > 6 || now - draw.startTick >= AUTO_FIRE_TICKS) {
                    drawing.remove(caster.getUniqueId());
                    cancel();
                    fire(caster, draw.tier,
                            Math.min(1.0, held / (double) FULL_DRAW_TICKS));
                    return;
                }
                // drawing the light in: slowed, gathering sparks
                caster.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 8, 1, true, false));
                double f = Math.min(1.0, (now - draw.startTick) / (double) FULL_DRAW_TICKS);
                caster.getWorld().spawnParticle(Particle.END_ROD,
                        caster.getEyeLocation().add(caster.getEyeLocation()
                                .getDirection().multiply(0.8)),
                        1 + (int) (f * 4), 0.25 * (1 - f) + 0.05, 0.25 * (1 - f) + 0.05,
                        0.25 * (1 - f) + 0.05, 0.005);
                if (f >= 1.0 && !draw.chimed) {
                    draw.chimed = true;
                    caster.playSound(caster.getLocation(),
                            Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
                } else if ((now - draw.startTick) % 8 == 0) {
                    caster.playSound(caster.getLocation(),
                            Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.9f + 1.1f * (float) f);
                }
            }
        }.runTaskTimer(plugin, 1, 1);
        return true;
    }

    private void fire(Player caster, int tier, double f) {
        double range = 12 + 12 * f;
        double damage = 3 + 5 * f;
        double smiteBonus = 2 + 4 * f;
        int glowTicks = (int) (20 * (4 + 4 * f));
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
        for (double d = 0.5; d < range; d += 0.4) {
            caster.getWorld().spawnParticle(Particle.END_ROD,
                    caster.getEyeLocation().clone().add(dir.clone().multiply(d)),
                    f >= 0.99 ? 2 : 1, 0.02, 0.02, 0.02, 0);
        }
        caster.getWorld().playSound(caster.getLocation(),
                Sound.ENTITY_ALLAY_ITEM_THROWN, 1.4f, 1.6f - 0.5f * (float) f);
        for (LivingEntity victim : struck) {
            boolean undead = Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType());
            victim.damage(undead ? damage + smiteBonus : damage, caster);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowTicks, 0));
            victim.getWorld().spawnParticle(Particle.FIREWORK,
                    victim.getEyeLocation(), 8 + (int) (10 * f), 0.25, 0.25, 0.25, 0.08);
        }
    }
}
