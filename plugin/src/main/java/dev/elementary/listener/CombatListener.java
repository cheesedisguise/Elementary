package dev.elementary.listener;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.shard.Shards;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** Damage-hook passives: fall handling, fire melee, nether bonus. */
public class CombatListener implements Listener {
    private final ElementaryPlugin plugin;

    public CombatListener(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerData holdingData(Player player) {
        if (!Shards.isShard(player.getInventory().getItemInOffHand())) return null;
        return plugin.shards().dataFor(player);
    }

    private final java.util.Map<java.util.UUID, Integer> staticCharge =
            new java.util.HashMap<>();

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

    private final java.util.Map<java.util.UUID, Long> lumenFlash = new java.util.HashMap<>();

    /** Lumen: melee attackers get flash-blinded (3s per-attacker cooldown). */
    @EventHandler(ignoreCancelled = true)
    public void onLightStruck(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        PlayerData data = holdingData(victim);
        if (data == null || data.element != Element.LIGHT) return;
        long now = System.currentTimeMillis();
        Long last = lumenFlash.get(attacker.getUniqueId());
        if (last != null && now - last < 3000) return;
        lumenFlash.put(attacker.getUniqueId(), now);
        attacker.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.BLINDNESS, 30, 0));
        victim.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                victim.getLocation().add(0, 1.2, 0), 8, 0.3, 0.3, 0.3, 0.06);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerData data = holdingData(attacker);
        if (data == null) return;
        if (data.element == Element.SHADOW
                && event.getEntity() instanceof LivingEntity victim
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // Umbra backstab: struck from behind
            org.bukkit.util.Vector facing = victim.getLocation().getDirection().setY(0);
            org.bukkit.util.Vector toVictim = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector()).setY(0);
            if (facing.lengthSquared() > 0.01 && toVictim.lengthSquared() > 0.01
                    && facing.normalize().dot(toVictim.normalize()) > 0.35) {
                event.setDamage(event.getDamage() + (data.tier >= 2 ? 4.0 : 2.0));
                victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                        victim.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);
            }
        }
        if (data.element == Element.LIGHTNING
                && event.getEntity() instanceof LivingEntity victim
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // Static: every 4th melee hit (3rd at tier 2) discharges
            int threshold = data.tier >= 2 ? 3 : 4;
            int count = staticCharge.merge(attacker.getUniqueId(), 1, Integer::sum);
            if (count >= threshold) {
                staticCharge.put(attacker.getUniqueId(), 0);
                event.setDamage(event.getDamage() + 2.0);
                victim.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                        victim.getEyeLocation(), 24, 0.3, 0.4, 0.3, 0.12);
                victim.getWorld().playSound(victim.getLocation(),
                        org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2f);
                plugin.challenges().staticDischarge(attacker);
            } else {
                victim.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK,
                        victim.getEyeLocation(), 3, 0.2, 0.3, 0.2, 0.04);
            }
        }
        if (data.element == Element.FIRE && event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 60));
            boolean nether = attacker.getWorld().getEnvironment() == World.Environment.NETHER;
            if (nether || data.tier >= 2) {
                event.setDamage(event.getDamage() + (nether ? 2.0 : 1.0));
            }
        }
    }
}
