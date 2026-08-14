package dev.elementary.item;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.msg.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {
    private final ElementaryPlugin plugin;

    public ItemListener(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (Items.isUpgrader(held)) {
            event.setCancelled(true);
            PlayerData data = plugin.shards().dataFor(player);
            if (data.tier >= 2) {
                Msg.fail(player, "You are already Tier 2");
                return;
            }
            held.subtract();
            plugin.shards().promote(player);
        } else if (Items.isTrader(held)) {
            event.setCancelled(true);
            PlayerData data = plugin.shards().dataFor(player);
            if (plugin.shards().boundElement(player.getUniqueId()) != null) {
                Msg.fail(player, "Your shard refuses the trade"); // bound players keep theirs
                return;
            }
            List<Element> pool = new ArrayList<>(List.of(Element.EARTH, Element.WATER,
                    Element.FIRE, Element.AIR));
            pool.remove(data.element);
            Element next = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            held.subtract();
            data.element = next;
            data.tier = 1;
            data.challengeProgress = 0;
            data.challengeMilestone = 0;
            plugin.store().save();
            // replace the shard item to match
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (dev.elementary.shard.Shards.isShard(player.getInventory().getItem(i))) {
                    player.getInventory().setItem(i, null);
                }
            }
            if (dev.elementary.shard.Shards.isShard(player.getInventory().getItemInOffHand())) {
                player.getInventory().setItemInOffHand(null);
            }
            plugin.shards().reissue(player);
            player.sendMessage(net.kyori.adventure.text.Component
                    .text("The trade is made: ", net.kyori.adventure.text.format
                            .NamedTextColor.GRAY)
                    .append(net.kyori.adventure.text.Component
                            .text(next.shardName(), next.color())));
        }
    }
}
