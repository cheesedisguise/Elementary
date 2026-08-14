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
        if (data.element == Element.FIRE && event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 60));
            boolean nether = attacker.getWorld().getEnvironment() == World.Environment.NETHER;
            if (nether || data.tier >= 2) {
                event.setDamage(event.getDamage() + (nether ? 2.0 : 1.0));
            }
        }
    }
}
