package dev.elementary.ability.light;

import dev.elementary.ability.Ability;
import dev.elementary.util.Targets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Consecrate: sanctified ground, 6 blocks around the cast spot, 10s.
 * Inside it the undead burn as if under the noon sun, mobs take +30%
 * damage (enemy players +15%), and mobs slain there drop double XP.
 * The farm zone - drop it over a spawner floor.
 */
public class Consecrate implements Ability, Listener {
    private record Zone(UUID owner, Location center, double radius, int endTick) {
        boolean active() { return Bukkit.getCurrentTick() < endTick; }
        boolean contains(Location at) {
            return at.getWorld().equals(center.getWorld())
                    && at.distanceSquared(center) <= radius * radius;
        }
    }

    private final JavaPlugin plugin;
    private final List<Zone> zones = new ArrayList<>();

    public Consecrate(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String name() { return "Consecrate"; }
    @Override public double cooldownSeconds(int tier) { return 30; }

    @Override
    public boolean cast(Player caster, int tier) {
        double radius = tier >= 2 ? 8 : 6;
        Zone zone = new Zone(caster.getUniqueId(), caster.getLocation().clone(), radius,
                Bukkit.getCurrentTick() + 20 * 10);
        zones.add(zone);
        Location center = zone.center();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.8f);
        new BukkitRunnable() {
            @Override public void run() {
                if (!zone.active()) {
                    zones.remove(zone);
                    center.getWorld().playSound(center,
                            Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.6f);
                    cancel();
                    return;
                }
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    center.getWorld().spawnParticle(Particle.DUST,
                            center.clone().add(radius * Math.cos(angle), 0.3,
                                    radius * Math.sin(angle)), 1, 0.08, 0.05, 0.08, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFE08A), 1.2f));
                }
                center.getWorld().spawnParticle(Particle.END_ROD,
                        center.clone().add(0, 0.6, 0), 2, radius * 0.5, 0.5, radius * 0.5,
                        0.004);
                if (Bukkit.getCurrentTick() % 20 != 0) return;
                Player owner = Bukkit.getPlayer(zone.owner());
                for (LivingEntity target : center.getNearbyLivingEntities(radius)) {
                    if (!zone.contains(target.getLocation())) continue;
                    if (target instanceof Player) continue;
                    if (owner != null && !Targets.hostile(owner, target)) continue;
                    if (Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(target.getType())) {
                        // holy ground: the undead burn as in daylight
                        target.setFireTicks(Math.max(target.getFireTicks(), 40));
                    }
                    target.addPotionEffect(new PotionEffect(
                            PotionEffectType.GLOWING, 30, 0, true, false));
                }
            }
        }.runTaskTimer(plugin, 0, 5);
        return true;
    }

    /** +30% damage to mobs inside, +15% to enemy players inside. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        for (Zone zone : zones) {
            if (!zone.active() || !zone.contains(victim.getLocation())) continue;
            if (victim instanceof Player player) {
                Player owner = Bukkit.getPlayer(zone.owner());
                if (owner != null && Targets.friendly(owner, player)) continue;
                event.setDamage(event.getDamage() * 1.15);
            } else {
                event.setDamage(event.getDamage() * 1.30);
            }
            return; // one blessing per hit, however many zones overlap
        }
    }

    /** Mobs slain on holy ground give up twice the experience. */
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        for (Zone zone : zones) {
            if (!zone.active() || !zone.contains(event.getEntity().getLocation())) continue;
            event.setDroppedExp(event.getDroppedExp() * 2);
            event.getEntity().getWorld().spawnParticle(Particle.END_ROD,
                    event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05);
            return;
        }
    }
}
