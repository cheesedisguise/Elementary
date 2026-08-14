package dev.elementary.item;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.msg.Msg;
import dev.elementary.shard.Shards;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The Shard Broker's picker: click an element, become it. */
public class BrokerMenu implements Listener {
    private final ElementaryPlugin plugin;

    public BrokerMenu(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    private static class Holder implements InventoryHolder {
        Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 9,
                Component.text("Choose your element"));
        holder.inventory = inv;
        Element[] elements = Element.values();
        PlayerData data = plugin.shards().dataFor(player);
        for (int i = 0; i < elements.length && i < 9; i++) {
            ItemStack display = Shards.create(elements[i], player.getUniqueId(), 1);
            var meta = display.getItemMeta();
            meta.lore(List.of(elements[i] == data.element
                    ? Component.text("Your current element.", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
                    : Component.text("Click to become " + elements[i].displayName() + ".",
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Element[] elements = Element.values();
        int slot = event.getSlot();
        if (slot < 0 || slot >= elements.length) return;
        Element chosen = elements[slot];
        PlayerData data = plugin.shards().dataFor(player);
        if (chosen == data.element) {
            Msg.fail(player, "That is already your element");
            return;
        }
        // consume one Broker; closing without choosing costs nothing
        ItemStack broker = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (Items.isBroker(item)) { broker = item; break; }
        }
        if (broker == null) {
            player.closeInventory();
            return;
        }
        broker.subtract();
        player.closeInventory();
        plugin.shards().applyElement(player, chosen);
        player.sendMessage(Component.text("The exchange is made: ", NamedTextColor.GRAY)
                .append(Component.text(chosen.shardName(), chosen.color())));
        player.playSound(player.getLocation(),
                org.bukkit.Sound.BLOCK_BARREL_CLOSE, 1f, 1.2f);
    }
}
