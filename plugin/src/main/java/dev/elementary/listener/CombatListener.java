package dev.elementary.listener;

import dev.elementary.ElementaryPlugin;
import dev.elementary.ability.lightning.Powerplant;
import dev.elementary.ability.shadow.Hunt;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.status.StatusService;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** Damage-hook passives: fall handling, backstabs, fear, zaps, statuses. */
public class CombatListener implements Listener {
    private final ElementaryPlugin plugin;

    public CombatListener(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerData holdingData(Player player) {
        // passive combat hooks apply all the time, whatever is held
        return plugin.shards().dataFor(player);
    }

    /** Run For Your Life's hit counter, per shadow player. */
    private final java.util.Map<java.util.UUID, Integer> dreadCount =
            new java.util.HashMap<>();

    /** Earth's stone plates listen for every wound. */
    @EventHandler(ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.passives().notePain(player);
        }
    }

    /** Charged: lightning never hurts a Lightning player. */
    @EventHandler(ignoreCancelled = true)
    public void onLightning(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = holdingData(player);
        if (data != null && data.element == Element.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = holdingData(player);
        if (data == null) return;
        if (data.element == Element.AIR) {
            event.setCancelled(true);
        } else if (data.element == Element.EARTH) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerData data = holdingData(attacker);
        if (data == null) return;
        if (data.element == Element.SHADOW
                && event.getEntity() instanceof LivingEntity victim
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // Run For Your Life: struck from behind cuts deeper
            org.bukkit.util.Vector facing = victim.getLocation().getDirection().setY(0);
            org.bukkit.util.Vector toVictim = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector()).setY(0);
            if (facing.lengthSquared() > 0.01 && toVictim.lengthSquared() > 0.01
                    && facing.normalize().dot(toVictim.normalize()) > 0.35) {
                event.setDamage(event.getDamage() + (data.tier >= 2 ? 4.0 : 2.0));
                victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                        victim.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);
            }
            // ...and every 4th hit (3rd at tier 2) plants the Fear
            int threshold = data.tier >= 2 ? 3 : 4;
            int count = dreadCount.merge(attacker.getUniqueId(), 1, Integer::sum);
            if (count >= threshold) {
                dreadCount.put(attacker.getUniqueId(), 0);
                plugin.status().apply(victim, StatusService.Status.FEAR, 35, attacker);
            }
        }
        if (data.element == Element.LIGHTNING
                && event.getEntity() instanceof LivingEntity victim
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // Over-Charged: at 12.5+ blocks/sec of Momentum, every hit
            // zaps - bonus damage, a blink of camera lock, the sound
            double charge = plugin.passives().momentum(attacker);
            if (charge >= 12.5) {
                boolean plant = Powerplant.active(attacker);
                double zap = plant ? 2.0 : data.tier >= 2 ? 1.0 : 0.5;
                event.setDamage(event.getDamage() + zap);
                if (victim instanceof Player) {
                    dev.elementary.util.CameraLock.hold(plugin, victim, 2, 0);
                }
                victim.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                        victim.getEyeLocation(), 20, 0.3, 0.4, 0.3, 0.1);
                victim.getWorld().playSound(victim.getLocation(),
                        org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.45f, 2f);
                plugin.challenges().staticDischarge(attacker);
            }
        }
        if (data.element == Element.FIRE && event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 40));
            boolean nether = attacker.getWorld().getEnvironment() == World.Environment.NETHER;
            if (nether || data.tier >= 2) {
                event.setDamage(event.getDamage() + 1.0);
            }
        }
    }

    /** The status layer: every hit is rescaled by what rides each side.
     *  Fear +15% in; Absolute Radiance +50% from Light hands; Luminosity
     *  / AR / Harmony scale the damager; Hunt +10% inside the storm. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatusScale(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        Player damager = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player p) {
                damager = p;
            } else if (byEntity.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player p) {
                damager = p;
            }
        }
        double mult = plugin.status().damageInMultiplier(victim, damager);
        if (damager != null) {
            mult *= plugin.status().damageOutMultiplier(damager);
            mult *= Hunt.damageMultiplier(damager, victim);
        }
        if (Math.abs(mult - 1.0) > 0.0001) {
            event.setDamage(event.getDamage() * mult);
        }
    }
}
