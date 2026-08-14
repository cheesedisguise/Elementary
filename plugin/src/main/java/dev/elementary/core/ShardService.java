package dev.elementary.core;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.shard.Shards;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Assignment, re-issue, promotion and demotion. */
public class ShardService {
    private static final List<Element> POOL =
            List.of(Element.EARTH, Element.WATER, Element.FIRE, Element.AIR);

    private final ElementaryPlugin plugin;

    public ShardService(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    /** The element this player must have, or null for the random pool. */
    public Element boundElement(UUID id) {
        Map<UUID, Element> bound = plugin.boundPlayers();
        return bound.get(id);
    }

    public PlayerData dataFor(Player player) {
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data == null) {
            Element bound = boundElement(player.getUniqueId());
            Element element = bound != null ? bound
                    : POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
            data = new PlayerData(element);
            plugin.store().put(player.getUniqueId(), data);
            player.sendMessage(Component.text("You are bound to the ", NamedTextColor.GRAY)
                    .append(Component.text(element.shardName(), element.color()))
                    .append(Component.text(".", NamedTextColor.GRAY)));
        } else {
            Element bound = boundElement(player.getUniqueId());
            if (bound != null && data.element != bound) {
                data.element = bound;
                data.tier = 1;
                plugin.store().save();
            }
        }
        return data;
    }

    /** Make sure the player carries exactly their shard. */
    public void ensureShard(Player player) {
        PlayerData data = dataFor(player);
        PlayerInventory inv = player.getInventory();
        boolean found = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (!Shards.isShard(item)) continue;
            if (found || Shards.element(item) != data.element || Shards.tier(item) != data.tier
                    || !player.getUniqueId().equals(Shards.owner(item))) {
                inv.setItem(i, null);
            } else {
                found = true;
            }
        }
        ItemStack off = inv.getItemInOffHand();
        if (Shards.isShard(off)) {
            found = true;
        }
        if (!found) {
            ItemStack shard = Shards.create(data.element, player.getUniqueId(), data.tier);
            if (off.getType().isAir()) {
                inv.setItemInOffHand(shard);
            } else {
                inv.addItem(shard);
            }
        }
    }

    public void reissue(Player player) {
        PlayerData data = dataFor(player);
        for (ItemStack item : player.getInventory().getContents()) {
            if (Shards.isShard(item)) return;
        }
        ItemStack shard = Shards.create(data.element, player.getUniqueId(), data.tier);
        if (player.getInventory().getItemInOffHand().getType().isAir()) {
            player.getInventory().setItemInOffHand(shard);
        } else {
            player.getInventory().addItem(shard);
        }
    }

    public void promote(Player player) {
        PlayerData data = dataFor(player);
        if (data.tier >= 2) return;
        data.tier = 2;
        plugin.store().save();
        swapShard(player, data);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (plugin.getConfig().getBoolean("announce-ascension", true)) {
            Bukkit.broadcast(Component.text("\u2726 ", NamedTextColor.LIGHT_PURPLE)
                    .append(player.displayName())
                    .append(Component.text(" has ascended \u2014 ", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(data.element.displayName() + " Tier 2",
                            data.element.color()).decorate(TextDecoration.BOLD)));
        }
    }

    /** Death demotion: tier 2 collapses back to a raw shard. */
    public void demote(Player player) {
        PlayerData data = dataFor(player);
        if (data.tier < 2) return;
        data.tier = 1;
        data.challengeProgress = 0;
        data.challengeMilestone = 0;
        plugin.store().save();
        swapShard(player, data);
    }

    private void swapShard(Player player, PlayerData data) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (Shards.isShard(inv.getItem(i))) {
                inv.setItem(i, Shards.create(data.element, player.getUniqueId(), data.tier));
                return;
            }
        }
        ItemStack off = inv.getItemInOffHand();
        if (Shards.isShard(off)) {
            inv.setItemInOffHand(Shards.create(data.element, player.getUniqueId(), data.tier));
        }
    }
}
