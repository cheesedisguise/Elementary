package dev.elementary.core;

import dev.elementary.ElementaryPlugin;
import dev.elementary.shard.Shards;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/** The shard can never leave its owner. */
public class LockdownListener implements Listener {
    private final ElementaryPlugin plugin;

    public LockdownListener(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.shards().ensureShard(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Shards.isShard(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(Shards::isShard);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.shards().reissue(event.getPlayer()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean movingShard = Shards.isShard(current) || Shards.isShard(cursor);
        if (!movingShard) return;
        // moves inside the player's own inventory are fine; anything
        // touching another inventory is not
        if (event.getView().getTopInventory().getSize() > 0
                && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick()
                && event.getClickedInventory() == event.getWhoClicked().getInventory()
                && event.getView().getTopInventory().getType()
                        != org.bukkit.event.inventory.InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!Shards.isShard(event.getOldCursor())) return;
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && event.getView().getTopInventory().getType()
                    != org.bukkit.event.inventory.InventoryType.CRAFTING) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!Shards.isShard(item)) return;
        if (!(event.getEntity() instanceof Player player)
                || !player.getUniqueId().equals(Shards.owner(item))) {
            event.setCancelled(true);
            return;
        }
        // the owner never carries two: a stray copy on the ground evaporates
        for (ItemStack held : player.getInventory().getContents()) {
            if (Shards.isShard(held)) {
                event.setCancelled(true);
                event.getItem().remove();
                return;
            }
        }
    }
}
