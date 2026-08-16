package dev.elementary.ability;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.msg.Msg;
import dev.elementary.shard.Shards;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/** Routes offhand-shard clicks to abilities: RMB, sneak-LMB, sneak-RMB. */
public class AbilityManager implements Listener {
    public record Kit(Ability primary, Ability secondary, Ability ultimate) {}

    private final ElementaryPlugin plugin;
    private final Map<Element, Kit> kits = new EnumMap<>(Element.class);

    public AbilityManager(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Element element, Kit kit) {
        kits.put(element, kit);
    }

    public Kit kit(Element element) { return kits.get(element); }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // passives follow you anywhere; casting demands a grip -
        // the three abilities need the shard in the MAIN hand
        if (!Shards.isShard(player.getInventory().getItemInMainHand())) return;
        // interact fires once per hand; only the main-hand event casts
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Action action = event.getAction();
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        PlayerData data = plugin.shards().dataFor(player);
        Kit kit = kits.get(data.element);
        if (kit == null) return;

        // a held-down right click feeds the charging ability, whatever
        // the sneak state - releasing the button is what fires it
        if (right && kit.primary() instanceof ChargedAbility charged
                && charged.charging(player)) {
            charged.feed(player);
            event.setCancelled(true);
            return;
        }

        Ability ability;
        if (right && player.isSneaking()) {
            ability = kit.ultimate();
        } else if (right) {
            ability = kit.primary();
        } else if (player.isSneaking()) {
            ability = kit.secondary();
        } else {
            return;
        }
        if (ability == null) return;
        // only claim the click once we know this is an ability attempt
        if (right) event.setCancelled(true);

        if (ability.ultimate() && data.tier < 2) {
            Msg.fail(player, "Tier 2 required");
            return;
        }
        String key = ability.name();
        long remaining = plugin.cooldowns().remainingMs(player.getUniqueId(), key);
        if (remaining > 0) {
            Msg.cooldown(player, remaining);
            return;
        }
        if (!ability.cast(player, data.tier)) return;
        plugin.cooldowns().set(player.getUniqueId(), key, ability.cooldownSeconds(data.tier));
        if (ability.ultimate()) {
            Msg.ultimate(player, data, ability.name());
        } else {
            Msg.used(player, data, ability.name());
        }
    }
}
