package dev.elementary.ability.aqua;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Live-syncs clone equipment and pops armour-stand clones on hit. */
public class MirageSyncListener implements Listener {
    private final JavaPlugin plugin;
    private final Mirage mirage;

    public MirageSyncListener(JavaPlugin plugin, Mirage mirage) {
        this.plugin = plugin;
        this.mirage = mirage;
    }

    private void resync(Player player) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> mirage.syncEquipment(player));
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) { resync(event.getPlayer()); }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) { resync(event.getPlayer()); }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) resync(player);
    }

    /** Armour-stand clones take real damage events; pop them there. */
    @EventHandler
    public void onStandHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (mirage.handleHit(attacker, event.getEntity().getEntityId())) {
            event.setCancelled(true);
        }
    }
}
