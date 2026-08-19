package dev.elementary.command;

import dev.elementary.ElementaryPlugin;
import dev.elementary.ability.Ability;
import dev.elementary.ability.AbilityManager;
import dev.elementary.data.PlayerData;
import dev.elementary.msg.Msg;
import dev.elementary.shard.Shards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * THE casting input: /ability1, /ability2 and /ultimate (alias /ult).
 * Clicks never cast - bind these commands to keys. The rules don't
 * bend for the chat box: shard in the MAIN hand, cooldowns, tier lock.
 */
public class AbilityCommand implements org.bukkit.command.CommandExecutor {
    private final ElementaryPlugin plugin;

    public AbilityCommand(ElementaryPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players cast abilities.",
                    NamedTextColor.RED));
            return true;
        }
        if (!Shards.isShard(player.getInventory().getItemInMainHand())) {
            Msg.fail(player, "Hold your shard in your main hand");
            return true;
        }
        PlayerData data = plugin.shards().dataFor(player);
        AbilityManager.Kit kit = plugin.abilities().kit(data.element);
        if (kit == null) return true;
        Ability ability = switch (command.getName().toLowerCase()) {
            case "ability1" -> kit.primary();
            case "ability2" -> kit.secondary();
            case "ultimate" -> kit.ultimate();
            default -> null;
        };
        if (ability == null) return true;
        plugin.abilities().tryCast(player, data, ability);
        return true;
    }
}
