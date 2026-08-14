package dev.elementary.command;

import dev.elementary.ElementaryPlugin;
import dev.elementary.data.PlayerData;
import dev.elementary.element.Element;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final ElementaryPlugin plugin;

    public AdminCommand(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.loadBoundPlayers();
            sender.sendMessage(Component.text("Elementary config reloaded.",
                    NamedTextColor.GRAY));
            return true;
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("set")) {
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            try {
                Element element = Element.valueOf(args[2].toUpperCase());
                int tier = Math.max(1, Math.min(2, Integer.parseInt(args[3])));
                PlayerData data = plugin.shards().dataFor(target);
                data.element = element;
                data.tier = tier;
                data.challengeProgress = 0;
                data.challengeMilestone = 0;
                plugin.store().save();
                for (int i = 0; i < target.getInventory().getSize(); i++) {
                    if (dev.elementary.shard.Shards.isShard(target.getInventory().getItem(i))) {
                        target.getInventory().setItem(i, null);
                    }
                }
                if (dev.elementary.shard.Shards.isShard(
                        target.getInventory().getItemInOffHand())) {
                    target.getInventory().setItemInOffHand(null);
                }
                plugin.shards().reissue(target);
                sender.sendMessage(Component.text("Set " + target.getName() + " to "
                        + element.displayName() + " tier " + tier, NamedTextColor.GRAY));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(Component.text(
                        "Usage: /elementary set <player> <element> <1|2>",
                        NamedTextColor.RED));
            }
            return true;
        }
        sender.sendMessage(Component.text(
                "Usage: /elementary <reload | set <player> <element> <1|2>>",
                NamedTextColor.RED));
        return true;
    }
}
