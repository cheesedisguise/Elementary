package dev.elementary.tier;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.item.Items;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Tier 2 is lost on death; the lost tier drops as an Upgrader. */
public class TierListener implements Listener {
    private final ElementaryPlugin plugin;

    public TierListener(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        PlayerData data = plugin.shards().dataFor(victim);
        if (data.tier < 2) return; // tier 1 deaths generate nothing
        plugin.shards().demote(victim);
        org.bukkit.Location at = victim.getLocation();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Item drop = at.getWorld().dropItemNaturally(at, Items.upgrader(plugin));
            drop.setGlowing(true);
            drop.setUnlimitedLifetime(true);
            drop.setPersistent(true);
        });
    }
}
