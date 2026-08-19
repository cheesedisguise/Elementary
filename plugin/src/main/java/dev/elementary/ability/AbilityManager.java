package dev.elementary.ability;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import dev.elementary.msg.Msg;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 * The kit registry and the single casting gate. Abilities are cast by
 * command only - /ability1, /ability2, /ultimate (AbilityCommand) -
 * clicking with the shard does nothing. Passives never route through
 * here at all.
 */
public class AbilityManager {
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

    /** The one gate every activation path goes through: tier lock,
     *  cooldown, cast, cooldown start, message. */
    public boolean tryCast(Player player, PlayerData data, Ability ability) {
        if (ability.ultimate() && data.tier < 2) {
            Msg.fail(player, "Tier 2 required");
            return false;
        }
        String key = ability.name();
        long remaining = plugin.cooldowns().remainingMs(player.getUniqueId(), key);
        if (remaining > 0) {
            Msg.cooldown(player, remaining);
            return false;
        }
        if (!ability.cast(player, data.tier)) return false;
        plugin.cooldowns().set(player.getUniqueId(), key, ability.cooldownSeconds(data.tier));
        if (ability.ultimate()) {
            Msg.ultimate(player, data, ability.name());
        } else {
            Msg.used(player, data, ability.name());
        }
        return true;
    }
}
