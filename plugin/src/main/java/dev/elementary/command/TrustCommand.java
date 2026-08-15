package dev.elementary.command;

import dev.elementary.ElementaryPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /trust [player] and /untrust <player> - your personal ally list. */
public class TrustCommand implements TabExecutor {
    private final ElementaryPlugin plugin;

    public TrustCommand(ElementaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        boolean untrust = command.getName().equalsIgnoreCase("untrust");
        if (args.length == 0) {
            if (untrust) {
                player.sendMessage(Component.text("Usage: /untrust <player>",
                        NamedTextColor.RED));
                return true;
            }
            Map<UUID, String> allies = plugin.trust().trusted(player.getUniqueId());
            if (allies.isEmpty()) {
                player.sendMessage(Component.text(
                        "You trust nobody. /trust <player> makes your abilities spare "
                                + "them - and your support heal them.", NamedTextColor.GRAY));
            } else {
                player.sendMessage(Component.text("Trusted: ", NamedTextColor.GRAY)
                        .append(Component.text(String.join(", ", allies.values()),
                                NamedTextColor.AQUA)));
            }
            return true;
        }
        if (untrust) {
            String removed = plugin.trust().untrust(player.getUniqueId(), args[0]);
            if (removed == null) {
                player.sendMessage(Component.text("You don't trust " + args[0] + ".",
                        NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text(removed + " is fair game again.",
                        NamedTextColor.GRAY));
            }
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found (they must be online): "
                    + args[0], NamedTextColor.RED));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You already trust yourself.",
                    NamedTextColor.RED));
            return true;
        }
        plugin.trust().trust(player.getUniqueId(), target.getUniqueId(), target.getName());
        player.sendMessage(Component.text("Trusted ", NamedTextColor.GRAY)
                .append(target.displayName().color(NamedTextColor.AQUA))
                .append(Component.text(" - your abilities spare them, your support "
                        + "includes them. Trust is one-way.", NamedTextColor.GRAY)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label,
                                      String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length != 1 || !(sender instanceof Player player)) return out;
        String prefix = args[0].toLowerCase(Locale.ROOT);
        if (command.getName().equalsIgnoreCase("untrust")) {
            for (String name : plugin.trust().trusted(player.getUniqueId()).values()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(name);
            }
        } else {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player)
                        && online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }
}
