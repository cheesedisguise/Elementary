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
            List.of(Element.EARTH, Element.WATER, Element.FIRE, Element.AIR,
                    Element.ICE, Element.SHADOW, Element.LIGHT, Element.LIGHTNING);

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
                // rebuild in place so older shards pick up format changes
                // (curse of vanishing, component tweaks)
                inv.setItem(i, Shards.create(data.element, player.getUniqueId(), data.tier));
                found = true;
            }
        }
        if (!found) {
            ItemStack shard = Shards.create(data.element, player.getUniqueId(), data.tier);
            if (inv.getItemInMainHand().getType().isAir()) {
                inv.setItemInMainHand(shard);
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
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            player.getInventory().setItemInMainHand(shard);
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
        // no chat broadcast - ascension announces itself through the
        // glowing shard, and the toast if the advancement datapack is in
        player.sendMessage(Component.text("\u2726 Ascended \u2014 ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(data.element.displayName() + " Tier 2",
                        data.element.color()).decorate(TextDecoration.BOLD)));
    }

    /** Reroll or exchange: become this element at tier 1, fresh progress. */
    public void applyElement(Player player, Element element) {
        PlayerData data = dataFor(player);
        data.element = element;
        data.tier = 1;
        data.challengeProgress = 0;
        data.challengeMilestone = 0;
        plugin.store().save();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (dev.elementary.shard.Shards.isShard(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
        if (dev.elementary.shard.Shards.isShard(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
        reissue(player);
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
        ItemStack main = inv.getItemInMainHand();
        if (Shards.isShard(main)) {
            inv.setItemInMainHand(Shards.create(data.element, player.getUniqueId(), data.tier));
        }
    }
}
