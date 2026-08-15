package dev.elementary.ability.shadow;

import dev.elementary.ElementaryPlugin;
import dev.elementary.msg.Msg;
import dev.elementary.util.Targets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Mark for Death: pick your prey. The marked target is revealed and
 * your backstab bonus against them doubles; if they die while marked,
 * Shadowstep's cooldown resets on the spot. The assassin's loop -
 * mark, step behind, knife, vanish, next.
 */
public class MarkForDeath implements dev.elementary.ability.Ability, Listener {
    private record Mark(UUID hunter, int endTick) {}

    /** victim -> the hunter stalking them. One mark per victim. */
    private static final Map<UUID, Mark> marks = new HashMap<>();

    private final ElementaryPlugin plugin;

    public MarkForDeath(ElementaryPlugin plugin) { this.plugin = plugin; }

    public static boolean marked(LivingEntity victim, Player hunter) {
        Mark mark = marks.get(victim.getUniqueId());
        return mark != null && mark.hunter().equals(hunter.getUniqueId())
                && Bukkit.getCurrentTick() < mark.endTick();
    }

    @Override public String name() { return "Mark for Death"; }
    @Override public double cooldownSeconds(int tier) { return 25; }

    @Override
    public boolean cast(Player caster, int tier) {
        Vector dir = caster.getEyeLocation().getDirection();
        RayTraceResult hit = caster.getWorld().rayTrace(caster.getEyeLocation(), dir, 20,
                FluidCollisionMode.NEVER, true, 0.9,
                e -> e instanceof LivingEntity le && Targets.hostile(caster, le));
        if (hit == null || !(hit.getHitEntity() instanceof LivingEntity victim)) {
            Msg.fail(caster, "No prey in sight");
            return false;
        }
        int duration = (tier >= 2 ? 12 : 8) * 20;
        int end = Bukkit.getCurrentTick() + duration;
        Mark mark = new Mark(caster.getUniqueId(), end);
        marks.put(victim.getUniqueId(), mark);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0,
                true, false));
        victim.getWorld().playSound(victim.getLocation(),
                Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.7f);
        new BukkitRunnable() {
            @Override public void run() {
                if (Bukkit.getCurrentTick() >= end || !victim.isValid()) {
                    // only clear our own mark - the victim may have
                    // been marked afresh by now
                    marks.remove(victim.getUniqueId(), mark);
                    cancel();
                    return;
                }
                double spin = Bukkit.getCurrentTick() * 0.4;
                victim.getWorld().spawnParticle(Particle.DUST,
                        victim.getLocation().add(0.7 * Math.cos(spin), 1.9,
                                0.7 * Math.sin(spin)), 1, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(Color.fromRGB(0x8F2030), 1.3f));
            }
        }.runTaskTimer(plugin, 0, 2);
        return true;
    }

    /** The mark is collected: the knife comes back sharpened. */
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Mark mark = marks.remove(event.getEntity().getUniqueId());
        if (mark == null || Bukkit.getCurrentTick() >= mark.endTick()) return;
        Player hunter = Bukkit.getPlayer(mark.hunter());
        if (hunter == null) return;
        plugin.cooldowns().set(hunter.getUniqueId(), "Shadowstep", 0);
        hunter.playSound(hunter.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 0.6f);
        hunter.sendActionBar(Component.text("The mark is collected — Shadowstep "
                + "is ready.", NamedTextColor.DARK_RED));
    }
}
